package bluegreen.manager.tasks;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvironmentTx;

public class EnvironmentBuildTask extends LocalShellTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBuildTask.class);

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

  @Override
  protected TaskStatus handleProcessTracking(Process process, boolean noop) throws InterruptedException, IOException {
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
