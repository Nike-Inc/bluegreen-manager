package bluegreen.manager.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvironmentTx;
import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.utils.ProcessBuilderAdapter;
import bluegreen.manager.utils.ProcessBuilderAdapterFactory;

/**
 * Includes functionality that separates the Environment build task from a normal shell task,
 * specifically that it needs the ability to parse the output and decipher the hostname and
 * IP address of the machine created by the shell task
 * <p/>
 * NOTE:  A lot of this junk can be moved to ShellTask once there is no such thing as RemoteShellTask anymore
 */
@Lazy
@Component
@Scope("prototype")
public class EnvironmentBuildTask extends ShellTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBuildTask.class);

  @Autowired
  private ProcessBuilderAdapterFactory processBuilderAdapterFactory;

  @Autowired
  private EnvironmentTx environmentTx;

  private String hostname;

  private String ipAddress;

  private String stageEnvironmentName;

  private Environment stageEnvironment;

  @Override
  public Task assign(int position,
                     String liveEnvName,
                     String stageEnvName,
                     ShellConfig shellConfig,
                     boolean isStageBuilt) {
    stageEnvironment = environmentTx.findNamedEnv(stageEnvName);
    this.stageEnvironmentName = stageEnvName;

    return super.assign(position, liveEnvName, stageEnvName, shellConfig, isStageBuilt);
  }

  /**
   * Runs a configurable command locally.
   * <p/>
   * Provides read access to the data model of liveEnv and stageEnv, by means of command token substitution.
   */
  @Override
  public TaskStatus process(boolean noop) {
    LOGGER.info("Launching local shell command" + noopRemark(noop));
    loadDataModel();
    TaskStatus taskStatus = TaskStatus.NOOP;
    if (!noop) {
      checkConfig();
      SubstituterResult command = stringSubstituter.substituteVariables(shellConfig.getCommand());
      String[] commandTokens = command.getSubstituted().split("\\s+");
      ProcessBuilderAdapter processBuilderAdapter = processBuilderAdapterFactory.create(commandTokens)
          .redirectErrorStream(true);
      LOGGER.info("Executing command '" + command.getExpurgated() + "'");
      StopWatch stopWatch = new StopWatch();
      Process process = null;
      try {
        stopWatch.start();
        process = processBuilderAdapter.start();
        taskStatus = handleProcessTracking(process, noop);
      } catch (IOException e) {
        LOGGER.error("Shell command failed: " + command.getExpurgated(), e);
        taskStatus = TaskStatus.ERROR;
      } catch (InterruptedException e) {
        LOGGER.error("Shell command interrupted: " + command.getExpurgated(), e);
        taskStatus = TaskStatus.ERROR;
      } finally {
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
  private String blockAndLogOutput(Process process) throws IOException, InterruptedException {
    // Yes, stdout is 'getInputStream'.
    StringBuilder sb = new StringBuilder();
    LOGGER.debug("---------- OUTPUT BEGINS ----------");
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
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
  private void closeProcessStreams(Process process) {
    if (process != null) {
      LOGGER.debug("Closing process i/o streams");
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getErrorStream());
      IOUtils.closeQuietly(process.getOutputStream());
    }
  }

  private TaskStatus handleProcessTracking(Process process, boolean noop) throws InterruptedException, IOException {
    String output = blockAndLogOutput(process);
    TaskStatus status = checkForErrors(output, process.exitValue());

    Pattern pattern = Pattern.compile(shellConfig.getRegexpSuccess());
    Matcher matcher = pattern.matcher(output);

    if (matcher.find()) {
      ipAddress = matcher.group(1);
      hostname = matcher.group(2);
      persistModel(noop);
    } else {
      LOGGER.warn("Failed to find IP/hostname in the output of bluegreen-build-environment");
      status = TaskStatus.ERROR;
    }

    return status;
  }

  /**
   * Attaches the applicationVm to the environment entity, then opens a transaction and persists them.
   */
  private void persistModel(boolean noop) {
    if (!noop) {
      ApplicationVm applicationVm = makeApplicationVm();
      LOGGER.debug("Persisting new applicationVm " + applicationVm.getHostname() + " in env " + stageEnvironmentName);
      stageEnvironment.addApplicationVm(applicationVm);
      applicationVm.setEnvironment(stageEnvironment);
      environmentTx.updateEnvironment(stageEnvironment); //Cascades to new applicationVm.
    }
  }

  private ApplicationVm makeApplicationVm() {
    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setHostname(hostname);
    applicationVm.setIpAddress(ipAddress);
    return applicationVm;
  }
}
