package bluegreen.manager.tasks;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.client.ssh.SshClient;
import bluegreen.manager.client.ssh.SshTarget;
import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvironmentTx;
import bluegreen.manager.substituter.StringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;
import bluegreen.manager.substituter.SubstituterResult;
import static bluegreen.manager.substituter.SubstitutionKeys.ENV_NAME;
import bluegreen.manager.utils.RegexHelper;
import bluegreen.manager.utils.ShellResult;
import bluegreen.manager.utils.ThreadSleeper;
import bluegreen.manager.utils.Waiter;
import bluegreen.manager.utils.WaiterParameters;

/**
 * Executes a long-running configurable command over ssh to a third-party system that knows how to
 * create an application vm.
 */
@Lazy
@Component
public class SshVmCreateTask extends ApplicationVmTask
{
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

  @Autowired
  private RegexHelper regexHelper;

  @Autowired
  private StringSubstituterFactory stringSubstituterFactory;

  private StringSubstituter initialStringSubstituter;

  public Task init(int position, String envName)
  {
    super.assign(position, envName, true/*createVm*/);
    this.initialStringSubstituter = makeInitialStringSubstituter(envName);
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
    execSshVmCreateCommand(noop);
    persistModel(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  @Override
  protected void loadDataModel()
  {
    super.loadDataModel();
    initialStringSubstituter.loadDataModel();
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
      SubstituterResult command = initialStringSubstituter.substituteVariables(sshVmCreateConfig.getInitialCommand());
      ShellResult result = sshClient.execCommand(command);
      applicationVm = waitTilVmIsAvailable(result);
    }
  }

  /**
   * Makes a zero-env string substituter based on the user-specified envName.
   * <p/>
   * Can't use OneEnvStringSubstituter because of its assertion that 1 applicationVm already exists!
   */
  private StringSubstituter makeInitialStringSubstituter(String envName)
  {
    Map<String, String> substitutions = new HashMap<String, String>();
    substitutions.put(ENV_NAME, envName);
    return stringSubstituterFactory.createZero(substitutions);
  }

  /**
   * Creates a Waiter using an ssh vm progress checker, and returns a transient ApplicationVm entity when done.
   * In case of error - never returns null, throws instead.
   */
  private ApplicationVm waitTilVmIsAvailable(ShellResult initialResult)
  {
    LOGGER.info(context() + "Waiting for applicationVm to become available");
    SshVmCreateProgressChecker progressChecker = new SshVmCreateProgressChecker(initialResult, context(),
        sshClient, sshTarget, sshVmCreateConfig, regexHelper, stringSubstituterFactory);
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
      LOGGER.debug("Persisting new applicationVm " + applicationVm.getHostname() + " in env " + envName);
      environment.addApplicationVm(applicationVm);
      applicationVm.setEnvironment(environment);
      environmentTx.updateEnvironment(environment); //Cascades to new applicationVm.
    }
  }

}
