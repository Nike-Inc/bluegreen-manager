package bluegreen.manager.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.domain.Application;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvironmentTx;

/**
 * Persists the definition of a new stage application based on an existing live application.
 * <p/>
 * For the case where stage application deployment had to be handled "out-of-band" with a local shell task or
 * remote ssh task.
 * <p/>
 * The purpose of registering the stage app is so bluegreen can freeze/thaw during Go-Live.  Separately you should
 * consider how to make the stage app visible on the network for purposes of integration test.
 */
@Lazy
@Component
public class RegisterApplicationTask extends TwoEnvTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterApplicationTask.class);

  @Autowired
  private EnvironmentTx environmentTx;

  public Task assign(int position, String liveEnvName, String stageEnvName)
  {
    super.assign(position, liveEnvName, stageEnvName);
    return this;
  }

  /**
   * Pretty simple, just makes a transient Application entity and persists it.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    Application stageApplication = defineStageApplication();
    persistModel(stageApplication, noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  /**
   * Makes a transient Application entity for stage based on the existing live application.
   * <p/>
   * There are some big assumptions here.  Stage should be "secret" so we're not expecting an ELB
   */
  private Application defineStageApplication()
  {
    Application stageApplication = new Application();
    stageApplication.setApplicationVm(stageApplicationVm);
    stageApplication.setScheme(liveApplication.getScheme());
    stageApplication.setHostname(stageApplicationVm.getHostname());
    stageApplication.setPort(liveApplication.getPort()); //Big assumption: same port in both envs.
    stageApplication.setUrlPath(liveApplication.getUrlPath()); //App should definitely have same client api in both envs.
    return stageApplication;
  }

  /**
   * Attaches the application to the stage applicationVm, then opens a transaction and persists them.
   */
  private void persistModel(Application stageApplication, boolean noop)
  {
    LOGGER.info(context(stageEnv) + "Registering stage application" + noopRemark(noop));
    if (!noop)
    {
      stageApplicationVm.addApplication(stageApplication);
      environmentTx.updateEnvironment(stageEnv); //Cascades to new application.
    }
  }
}
