package com.nike.tools.bgm.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.substituter.StringSubstituter;
import com.nike.tools.bgm.substituter.StringSubstituterFactory;
import com.nike.tools.bgm.utils.ProcessBuilderAdapter;
import com.nike.tools.bgm.utils.ProcessBuilderAdapterFactory;

/**
 * Runs a configurable command locally that requires read access to the data model of liveEnv and stageEnv.
 * Assumes stageEnv vms have been created already.
 * <p/>
 * Command is expected to run in the foreground, and does not provide a "checking service" to report on progress,
 * hence there is no Waiter or ProgressChecker here.  No analysis is performed on the results except to know whether
 * the command succeeded or failed.
 * <p/>
 * Currently no support for timeout.
 */
@Lazy
@Component
@Scope("prototype")
public class LocalShellTask extends TaskImpl
{
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalShellTask.class);

  @Autowired
  private ProcessBuilderAdapterFactory processBuilderAdapterFactory;

  @Autowired
  private StringSubstituterFactory stringSubstituterFactory;

  private LocalShellConfig localShellConfig;
  private Pattern patternError;
  private StringSubstituter stringSubstituter;

  /**
   * Two-env shell task.
   */
  public Task assign(int position, String liveEnvName, String stageEnvName, LocalShellConfig localShellConfig)
  {
    super.assign(position);
    this.localShellConfig = localShellConfig;
    if (StringUtils.isNotBlank(localShellConfig.getRegexpError()))
    {
      this.patternError = Pattern.compile(localShellConfig.getRegexpError());
    }
    this.stringSubstituter = stringSubstituterFactory.createTwo(liveEnvName, stageEnvName,
        localShellConfig.getExtraSubstitutions());
    return this;
  }

  /**
   * One-env shell task.
   */
  public Task assign(int position, String envName, LocalShellConfig localShellConfig)
  {
    super.assign(position);
    this.localShellConfig = localShellConfig;
    if (StringUtils.isNotBlank(localShellConfig.getRegexpError()))
    {
      this.patternError = Pattern.compile(localShellConfig.getRegexpError());
    }
    this.stringSubstituter = stringSubstituterFactory.createOne(envName, localShellConfig.getExtraSubstitutions());
    return this;
  }

  private void loadDataModel()
  {
    stringSubstituter.loadDataModel();
  }

  /**
   * Runs a configurable command locally.
   * <p/>
   * Provides read access to the data model of liveEnv and stageEnv, by means of command token substitution.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    LOGGER.info("Launching local shell command" + noopRemark(noop));
    loadDataModel();
    TaskStatus taskStatus = TaskStatus.NOOP;
    if (!noop)
    {
      checkConfig();
      String[] commandTokens = stringSubstituter.substituteVariables(localShellConfig.getCommand()).split("\\s+");
      ProcessBuilderAdapter processBuilderAdapter = processBuilderAdapterFactory.create(commandTokens)
          .redirectErrorStream(true);
      LOGGER.info("Executing command '" + StringUtils.join(commandTokens, " ") + "'");
      StopWatch stopWatch = new StopWatch();
      Process process = null;
      try
      {
        stopWatch.start();
        process = processBuilderAdapter.start();
        String output = blockAndLogOutput(process);
        taskStatus = checkForErrors(output, process.exitValue());
      }
      catch (IOException e)
      {
        LOGGER.error("Shell command failed: " + StringUtils.join(commandTokens, " "), e);
        taskStatus = TaskStatus.ERROR;
      }
      catch (InterruptedException e)
      {
        LOGGER.error("Shell command interrupted: " + StringUtils.join(commandTokens, " "), e);
        taskStatus = TaskStatus.ERROR;
      }
      finally
      {
        stopWatch.stop();
        LOGGER.debug("Time elapsed: " + stopWatch);
        closeProcessStreams(process);
      }
    }
    return taskStatus;
  }

  /**
   * Checks that the local shell config gives a way to verify success or failure of the process run.
   */
  private void checkConfig()
  {
    if (localShellConfig.getExitvalueSuccess() == null && StringUtils.isBlank(localShellConfig.getRegexpError()))
    {
      throw new IllegalArgumentException("Configuration should specify one or both of exitvalueSuccess and regexpError");
    }
  }

  /**
   * Iterates over the process stdout until there is no more.  Blocks til the process is done.
   * Returns the output as a single string.
   * <p/>
   * Also logs the process exit value.
   */
  private String blockAndLogOutput(Process process) throws IOException, InterruptedException
  {
    // Yes, stdout is 'getInputStream'.
    StringBuilder sb = new StringBuilder();
    LOGGER.debug("---------- OUTPUT BEGINS ----------");
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null)
    {
      LOGGER.debug(line);
      sb.append(line + "\n");
    }
    LOGGER.debug("---------- OUTPUT ENDS ----------");
    logExitValue(process.waitFor());
    return sb.toString();
  }

  /**
   * Checks the process output and exitValue and returns DONE (success) or ERROR.
   */
  private TaskStatus checkForErrors(String output, int exitValue)
  {
    return checkOutput(output) && checkExitValue(exitValue) ? TaskStatus.DONE : TaskStatus.ERROR;
  }

  /**
   * True if output looks ok, or if error-regexp hasn't been defined.
   * False if output matches the error-regexp.
   */
  private boolean checkOutput(String output)
  {
    return patternError == null || !patternError.matcher(output).find();
  }

  /**
   * True if exitValue looks like success, or if success hasn't been defined.
   * <p/>
   * False only if success was defined and this exitValue is something else.
   */
  private boolean checkExitValue(int exitValue)
  {
    return localShellConfig.getExitvalueSuccess() == null || localShellConfig.getExitvalueSuccess() == exitValue;
  }

  /**
   * Logs the exit value, remarking on success/failure if the local shell config has defined the success value.
   */
  private void logExitValue(int exitValue)
  {
    if (localShellConfig.getExitvalueSuccess() == null)
    {
      LOGGER.debug("Command exit code: " + exitValue);
    }
    else
    {
      boolean success = localShellConfig.getExitvalueSuccess() == exitValue;
      LOGGER.debug("Command exit code: " + exitValue + " " + (success ? "(success)" : "(failure)"));
    }
  }

  /**
   * Closes all i/o streams, whether used or not.  It's not clear whether this is necessary after waitFor,
   * but better safe than sorry.
   */
  private void closeProcessStreams(Process process)
  {
    if (process != null)
    {
      LOGGER.debug("Closing process i/o streams");
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getErrorStream());
      IOUtils.closeQuietly(process.getOutputStream());
    }
  }
}
