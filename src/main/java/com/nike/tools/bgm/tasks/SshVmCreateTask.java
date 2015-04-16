package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.Waiter;
import com.nike.tools.bgm.utils.WaiterParameters;

/**
 * Runs a configurable command over ssh to a third-party system that knows how to
 * create an application vm.
 */
@Lazy
@Component
public class SshVmCreateTask extends ApplicationVmTask
{
  private static final String CMDVAR_ENVNAME = "%{envName}";

  private static final Logger LOGGER = LoggerFactory.getLogger(SshVmCreateTask.class);

  @Autowired
  @Qualifier("sshVmCreateTask")
  private WaiterParameters waiterParameters;

  @Autowired
  private ThreadSleeper threadSleeper;

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private SshTarget sshTarget;

  @Autowired
  private SshVmCreateConfig sshVmCreateConfig;

  @Autowired
  private SshClient sshClient;

  public Task init(int position, String envName)
  {
    super.init(position, envName, true/*createVm*/);
    return this;
  }

  /**
   * Runs a command over ssh on a third-party host that knows how to create an application vm.
   * <p/>
   * Persists the record of this new vm in the current environment.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    initSshClient(noop);
    execSshVmCreateCommand(noop);
    persistModel(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  private void initSshClient(boolean noop)
  {
    if (!noop)
    {
      sshClient.init(sshTarget);
    }
  }

  /**
   * Executes the initial command to create a vm.
   */
  void execSshVmCreateCommand(boolean noop)
  {
    LOGGER.info(context() + "Executing vm-create command over ssh" + noopRemark(noop));
    if (!noop)
    {
      String command = substituteInitialVariables(sshVmCreateConfig.getInitialCommand());
      String output = sshClient.execCommand(command);
      applicationVm = waitTilVmIsAvailable(output);
    }
  }

  /**
   * Substitutes variables of the form '%{vblname}' in the original string, returns the replaced version.
   * Currently supports only one variable: envName
   * <p/>
   * Can't support '${..}' since Spring already substitutes that in properties file.
   */
  private String substituteInitialVariables(String original)
  {
    return StringUtils.replace(original, CMDVAR_ENVNAME, environment.getEnvName());
  }

  /**
   * Creates a Waiter using an ssh vm progress checker, and returns a transient ApplicationVm entity when done.
   * In case of error - never returns null, throws instead.
   */
  private ApplicationVm waitTilVmIsAvailable(String initialOutput)
  {
    LOGGER.info(context() + "Waiting for applicationVm to become available");
    SshVmCreateProgressChecker progressChecker = new SshVmCreateProgressChecker(initialOutput, context(),
        sshClient, sshTarget, sshVmCreateConfig);
    Waiter<ApplicationVm> waiter = new Waiter(waiterParameters, threadSleeper, progressChecker);
    applicationVm = waiter.waitTilDone();
    if (applicationVm == null)
    {
      throw new RuntimeException(context() + progressChecker.getDescription() + " did not become available");
    }
    return applicationVm;
  }

  /**
   * Attaches the applicationVm to the environment entity, then opens a transaction and persists them.
   */
  private void persistModel(boolean noop)
  {
    if (!noop)
    {
      environment.addApplicationVm(applicationVm);
      applicationVm.setEnvironment(environment);
      environmentTx.updateEnvironment(environment); //Cascades to new applicationVm.
    }
  }

}
