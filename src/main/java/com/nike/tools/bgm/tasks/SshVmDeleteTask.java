package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.substituter.StringSubstituter;
import com.nike.tools.bgm.substituter.StringSubstituterFactory;
import com.nike.tools.bgm.substituter.SubstituterResult;
import com.nike.tools.bgm.utils.ShellResult;

/**
 * Executes a command over ssh to a third-party system that knows how to delete an application vm.
 * <p/>
 * TODO - this should be two tasks, one for remote cmd execution and one for model update
 */
@Lazy
@Component
public class SshVmDeleteTask extends ApplicationVmTask
{
  private static final String CMDVAR_ENVNAME = "%{envName}";

  private static final Logger LOGGER = LoggerFactory.getLogger(SshVmCreateTask.class);

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private SshTarget sshTarget;

  @Autowired
  private SshVmDeleteConfig sshVmDeleteConfig;

  @Autowired
  private SshClient sshClient;

  @Autowired
  private StringSubstituterFactory stringSubstituterFactory;

  private StringSubstituter stringSubstituter;

  public Task init(int position, String envName)
  {
    super.assign(position, envName, false/*i.e. modify vm*/);
    this.stringSubstituter = stringSubstituterFactory.createOne(envName, null/*no extra substitutions*/);
    return this;
  }

  /**
   * a
   * Runs a command over ssh on a third-party host that knows how to create an application vm.
   * <p/>
   * Persists the record of this new vm in the current environment.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    initSshClient(noop);
    execSshVmDeleteCommand(noop);
    persistModel(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  @Override
  protected void loadDataModel()
  {
    super.loadDataModel();
    stringSubstituter.loadDataModel();
  }

  private void initSshClient(boolean noop)
  {
    if (!noop)
    {
      sshClient.init(sshTarget);
    }
  }

  /**
   * Executes the initial command to delete a vm.  No waiting around afterwards.
   */
  private void execSshVmDeleteCommand(boolean noop)
  {
    LOGGER.info(context() + "Executing vm-delete command over ssh" + noopRemark(noop));
    if (!noop)
    {
      SubstituterResult command = stringSubstituter.substituteVariables(sshVmDeleteConfig.getInitialCommand());
      ShellResult result = sshClient.execCommand(command);
      checkDeleted(result);
    }
  }

  /**
   * Checks that there were no errors in the deletion result.
   * <p/>
   * Currently checks by exitvalue, and ignores stdout.
   */
  private void checkDeleted(ShellResult result)
  {
    LOGGER.debug("Command Output:\n" + result.describe());
    if (result.getExitValue() != sshVmDeleteConfig.getInitialExitvalueSuccess())
    {
      throw new RuntimeException("Expected exitValue " + sshVmDeleteConfig.getInitialExitvalueSuccess()
          + ", received exitValue " + result.getExitValue());
    }
  }

  /**
   * Removes the applicationVm from the environment entity, then opens a transaction and persists it as a delete.
   */
  private void persistModel(boolean noop)
  {
    if (!noop)
    {
      LOGGER.debug("Persisting removal of applicationVm " + applicationVm.getHostname() + " from env " + envName);
      environment.removeApplicationVm(applicationVm);
      environmentTx.updateEnvironment(environment); //Cascades to delete applicationVm.
    }
  }

}
