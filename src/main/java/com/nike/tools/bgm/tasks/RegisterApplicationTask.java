package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.TaskStatus;

/**
 * Persists the definition of a new stage application based on an existing live application.
 * <p/>
 * For the case where stage application deployment had to be handled "out-of-band" with a local shell task or
 * remote ssh task.
 */
@Lazy
@Component
public class RegisterApplicationTask extends TwoEnvTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterApplicationTask.class);

  public Task init(int position, String liveEnvName, String stageEnvName)
  {
    super.init(position, liveEnvName, stageEnvName);
    return this;
  }

  /**
   * Pretty simple, just makes a transient Application entity and persists it.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    Application stageApplication = defineStageApplication();
    persistModel(stageApplication, noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  /**
   * Makes a transient Application entity for stage based on the existing live application.
   */
  private Application defineStageApplication()
  {
    Application stageApplication = new Application();
    stageApplication.setApplicationVm(stageApplicationVm);
    stageApplication.setScheme(liveApplication.getScheme());
    stageApplication.setHostname(stageApplicationVm.getHostname()); //TODO: support ELB situations
    stageApplication.setPort(liveApplication.getPort()); //Big assumption: same port in both envs.
    stageApplication.setUrlPath(liveApplication.getUrlPath()); //App should definitely have same client api in both envs.
    return stageApplication;
  }

  /**
   * Attaches the application to the stage applicationVm, then opens a transaction and persists them.
   */
  private void persistModel(Application stageApplication, boolean noop)
  {
    LOGGER.info(context(stageEnv) + "Registering stage application");
    if (!noop)
    {
      stageApplicationVm.addApplication(stageApplication);
      environmentTx.updateEnvironment(stageEnv); //Cascades to new application.
    }
  }
}
