package bluegreen.manager.tasks;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.client.ssh.SshClient;
import bluegreen.manager.client.ssh.SshTarget;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvironmentTx;
import bluegreen.manager.substituter.StringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;
import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.utils.RegexHelper;
import bluegreen.manager.utils.ShellResult;

/**
 * Executes a command over ssh to a third-party system that knows how to delete an application vm.
 */
@Lazy
@Component
public class SshVmDeleteTask extends ApplicationVmTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SshVmDeleteTask.class);

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private SshTarget sshTarget;

  @Autowired
  private SshVmDeleteConfig sshVmDeleteConfig;

  @Autowired
  private SshClient sshClient;

  @Autowired
  private RegexHelper regexHelper;

  @Autowired
  private StringSubstituterFactory stringSubstituterFactory;

  private StringSubstituter stringSubstituter;
  private Pattern initialPatternSuccess;

  public Task init(int position, String envName)
  {
    super.assign(position, envName, false/*i.e. modify vm*/);
    this.stringSubstituter = stringSubstituterFactory.createOne(envName, null/*no extra substitutions*/);
    this.initialPatternSuccess = Pattern.compile(sshVmDeleteConfig.getInitialRegexpSuccess());
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
   * Checks that the deletion result output indicated success.
   * <p/>
   * Currently checks by stdout, and ignores exitValue (because ssh library Ganymed does not reliably return it).
   */
  private void checkDeleted(ShellResult result)
  {
    LOGGER.debug("Command Output:\n" + result.describe());
    if (!regexHelper.matcherFind(result.getOutput(), initialPatternSuccess))
    {
      throw new RuntimeException(context() + "FAILED: " + result.getOutput());
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
