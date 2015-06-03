package bluegreen.manager.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import bluegreen.manager.client.app.ApplicationClient;
import bluegreen.manager.client.app.ApplicationClientFactory;
import bluegreen.manager.client.app.ApplicationSession;
import bluegreen.manager.model.domain.Application;

/**
 * A task that communicates with an existing application, in an applicationVm, in an environment.
 */
public abstract class ApplicationTask extends ApplicationVmTask
{
  @Autowired
  protected ApplicationClientFactory applicationClientFactory;

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
    this.applicationClient = applicationClientFactory.create(application.getUsername(), application.getPassword());
    this.applicationSession = applicationClient.authenticate(application);
  }

}
