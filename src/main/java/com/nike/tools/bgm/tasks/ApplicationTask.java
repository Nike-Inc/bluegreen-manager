package com.nike.tools.bgm.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.model.domain.Application;

/**
 * A task that communicates with an existing application, in an applicationVm, in an environment.
 */
public abstract class ApplicationTask extends ApplicationVmTask
{
  @Autowired
  protected ApplicationClient applicationClient;

  protected ApplicationSession applicationSession;

  protected Application application;

  public Task assign(int position, String envName)
  {
    super.assign(position, envName, false/*createVm*/);
    return this;
  }

  /**
   * Loads datamodel entities and asserts preconditions on them.  These assertions should be true at the moment when
   * this task is about to begin processing.
   * <p/>
   * Looks up the environment entity by name.
   * Currently requires that the env has exactly one applicationVm and one application.
   */
  @Override
  protected void loadDataModel()
  {
    this.oneEnvLoader = envLoaderFactory.createOne(envName);
    oneEnvLoader.loadApplication();
    this.environment = oneEnvLoader.getEnvironment();
    this.applicationVm = oneEnvLoader.getApplicationVm();
    this.application = oneEnvLoader.getApplication();
  }

  /**
   * Initializes an authenticated session with the application.
   * <p/>
   * Could be called later than {@link #assign(int, String)}.
   */
  void initApplicationSession()
  {
    applicationSession = applicationClient.authenticate(application);
  }

}
