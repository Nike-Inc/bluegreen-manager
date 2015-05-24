package bluegreen.manager.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.utils.ProcessBuilderAdapter;
import bluegreen.manager.utils.ProcessBuilderAdapterFactory;

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
public class LocalShellTask extends ShellTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalShellTask.class);

  @Autowired
  private ProcessBuilderAdapterFactory processBuilderAdapterFactory;

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
      SubstituterResult command = stringSubstituter.substituteVariables(shellConfig.getCommand());
      String[] commandTokens = command.getSubstituted().split("\\s+");
      ProcessBuilderAdapter processBuilderAdapter = processBuilderAdapterFactory.create(commandTokens)
          .redirectErrorStream(true);
      LOGGER.info("Executing command '" + command.getExpurgated() + "'");
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
        LOGGER.error("Shell command failed: " + command.getExpurgated(), e);
        taskStatus = TaskStatus.ERROR;
      }
      catch (InterruptedException e)
      {
        LOGGER.error("Shell command interrupted: " + command.getExpurgated(), e);
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
