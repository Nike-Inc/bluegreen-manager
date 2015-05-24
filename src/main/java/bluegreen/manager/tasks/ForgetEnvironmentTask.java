package bluegreen.manager.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.EnvironmentTx;
import bluegreen.manager.model.tx.OneEnvLoader;

/**
 * Deletes the environment from the bluegreen datamodel.
 * <p/>
 * Has no impact on actual vms or databases.
 */
@Lazy
@Component
public class ForgetEnvironmentTask extends TaskImpl
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ForgetEnvironmentTask.class);

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private OneEnvLoader oneEnvLoader;

  private String envName;
  private Environment environment;

  public Task assign(int position, String envName)
  {
    super.assign(position);
    this.envName = envName;
    return this;
  }

  /**
   * Loads datamodel entities and asserts preconditions on them.  These assertions should be true at the moment when
   * this task is about to begin processing.
   * <p/>
   * Looks up the environment entities by name.
   * Actually - no preconditions, other than that the env already exists.
   */
  private void loadDataModel()
  {
    this.oneEnvLoader = envLoaderFactory.createOne(envName);
    oneEnvLoader.loadEnvironmentSimple();
    this.environment = oneEnvLoader.getEnvironment();
  }

  /**
   * Deletes the rds instance, its parameter group (if non-default), and its original bluegreen snapshot.
   * <p/>
   * Leaves behind any snapshots that Amazon automatically made of the rds instance.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    persistModel(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  /**
   * Opens a transaction to persist the removal of the environment entity.
   */
  private void persistModel(boolean noop)
  {
    LOGGER.info("Unregistering environment '" + envName + "'" + noopRemark(noop));
    if (!noop)
    {
      environmentTx.deleteEnvironment(environment); //Cascades to associations.
    }
  }

}
