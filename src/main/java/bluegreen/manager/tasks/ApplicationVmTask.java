package bluegreen.manager.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.OneEnvLoader;

/**
 * A task that communicates with an applicationVm, in an existing environment.
 */
public abstract class ApplicationVmTask extends TaskImpl
{
  @Autowired
  protected EnvLoaderFactory envLoaderFactory;

  protected String envName;
  private boolean createVm;

  protected OneEnvLoader oneEnvLoader;

  protected Environment environment;
  protected ApplicationVm applicationVm;

  /**
   * @param createVm True if this task is intended to create the applicationVm, false if it is expected to already exist.
   */
  protected Task assign(int position, String envName, boolean createVm)
  {
    super.assign(position);
    this.envName = envName;
    this.createVm = createVm;
    return this;
  }

  /**
   * Loads datamodel entities and asserts preconditions on them.  These assertions should be true at the moment when
   * this task is about to begin processing.
   * <p/>
   * Looks up the environment entity by name.
   * Currently requires that the env has exactly one applicationVm.
   */
  protected void loadDataModel()
  {
    this.oneEnvLoader = envLoaderFactory.createOne(envName);
    oneEnvLoader.loadApplicationVm(createVm);
    this.environment = oneEnvLoader.getEnvironment();
    this.applicationVm = oneEnvLoader.getApplicationVm();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   */
  String context()
  {
    return oneEnvLoader.context();
  }

}
