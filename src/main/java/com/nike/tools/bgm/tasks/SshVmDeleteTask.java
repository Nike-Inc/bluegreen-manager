package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshClientResult;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvironmentTx;

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

  public Task init(int position, String envName)
  {
    super.assign(position, envName, false/*i.e. modify vm*/);
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
      String command = substituteInitialVariables(sshVmDeleteConfig.getInitialCommand());
      SshClientResult result = sshClient.execCommand(command);
      checkDeleted(result);
    }
  }

  /**
   * Substitutes variables of the form '%{vblname}' in the original string, returns the replaced version.
   * Currently supports only one variable: envName
   * <p/>
   * Can't support '${..}' since Spring already substitutes that in properties file.
   * <p/>
   * TODO - Start using StringSubstituter
   */
  private String substituteInitialVariables(String original)
  {
    return StringUtils.replace(original, CMDVAR_ENVNAME, environment.getEnvName());
  }

  /**
   * Checks that there were no errors in the deletion result.
   * <p/>
   * Currently checks by exitvalue, and ignores stdout.
   */
  private void checkDeleted(SshClientResult result)
  {
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
      environment.removeApplicationVm(applicationVm);
      environmentTx.updateEnvironment(environment); //Cascades to delete applicationVm.
    }
  }

}
