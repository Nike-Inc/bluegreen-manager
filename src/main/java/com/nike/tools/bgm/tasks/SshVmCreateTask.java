package com.nike.tools.bgm.tasks;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.substituter.StringSubstituter;
import com.nike.tools.bgm.substituter.StringSubstituterFactory;
import com.nike.tools.bgm.substituter.SubstituterResult;
import com.nike.tools.bgm.utils.ShellResult;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.Waiter;
import com.nike.tools.bgm.utils.WaiterParameters;

import static com.nike.tools.bgm.substituter.SubstitutionKeys.ENV_NAME;

/**
 * Executes a long-running configurable command over ssh to a third-party system that knows how to
 * create an application vm.
 * <p/>
 * TODO - this should be two tasks, one for remote cmd execution and one for model update
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
    super.loadDataModel(); //TODO - Duplicates work of initialStringSubstituter.loadDataModel(), see if we can avoid this
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

  private StringSubstituter makeInitialStringSubstituter(String envName)
  {
    Map<String, String> substitutions = new HashMap<String, String>();
    substitutions.put(ENV_NAME, envName);
    return stringSubstituterFactory.createOne(envName, substitutions);
  }

  /**
   * Creates a Waiter using an ssh vm progress checker, and returns a transient ApplicationVm entity when done.
   * In case of error - never returns null, throws instead.
   */
  private ApplicationVm waitTilVmIsAvailable(ShellResult initialResult)
  {
    LOGGER.info(context() + "Waiting for applicationVm to become available");
    SshVmCreateProgressChecker progressChecker = new SshVmCreateProgressChecker(initialResult, context(),
        sshClient, sshTarget, sshVmCreateConfig, stringSubstituterFactory);
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
