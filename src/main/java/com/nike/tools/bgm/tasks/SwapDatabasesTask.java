package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.DatabaseType;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvironmentTx;

/**
 * Swaps the physical databases used by the stage env and live env.  Upon completion we have stage pointing to the
 * live db and live env pointing to the test (stage) db.
 * <p/>
 * This is just a change to the bluegreen datamodel, it does not affect any running applications.
 */
@Lazy
@Component
public class SwapDatabasesTask extends TwoEnvTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SwapDatabasesTask.class);

  @Autowired
  private EnvironmentTx environmentTx;

  private PhysicalDatabase newLivePhysicalDatabase;
  private PhysicalDatabase oldLivePhysicalDatabase;

  /**
   * Associates the stage environment with the live database.
   * <p/>
   * This is just a change to the bluegreen datamodel, it does not affect any running applications.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    assertPhysicalDatabaseLiveness();
    swapPhysicalDatabaseLinks();
    persistModel(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  @Override
  protected void loadDataModel()
  {
    super.loadDataModel();
    /*
     * Changes the frame of reference from live/stage to oldLive/newLive.
     */
    this.oldLivePhysicalDatabase = livePhysicalDatabase;
    this.newLivePhysicalDatabase = stagePhysicalDatabase;
  }

  /**
   * Precondition before this task makes any changes.
   * Checks that the old env's physicaldb is live and the new env's physicaldb is stage (non-live).
   */
  private void assertPhysicalDatabaseLiveness()
  {
    if (!oldLivePhysicalDatabase.isLive())
    {
      throw new IllegalStateException(context(liveEnv) + oldLivePhysicalDatabase + ": Expected physicaldb "
          + "to be initially live since it is in the current (old) live environment");
    }
    if (newLivePhysicalDatabase.isLive())
    {
      throw new IllegalStateException(context(stageEnv) + newLivePhysicalDatabase + ": Expected physicaldb "
          + "to be initially non-live since it is in the new live (i.e. stage) environment");
    }
  }

  /**
   * Swaps the physicaldb content of newLive and oldLive.  Excludes id and logicaldb parent fields.
   */
  private void swapPhysicalDatabaseLinks()
  {
    DatabaseType oldLiveDatabaseType = oldLivePhysicalDatabase.getDatabaseType();
    String oldLiveInstanceName = oldLivePhysicalDatabase.getInstanceName();
    boolean oldLiveIsLive = oldLivePhysicalDatabase.isLive();
    String oldLiveDriverClassName = oldLivePhysicalDatabase.getDriverClassName();
    String oldLiveUrl = oldLivePhysicalDatabase.getUrl();
    String oldLiveUsername = oldLivePhysicalDatabase.getUsername();
    String oldLivePassword = oldLivePhysicalDatabase.getPassword();

    oldLivePhysicalDatabase.setDatabaseType(newLivePhysicalDatabase.getDatabaseType());
    oldLivePhysicalDatabase.setInstanceName(newLivePhysicalDatabase.getInstanceName());
    oldLivePhysicalDatabase.setLive(newLivePhysicalDatabase.isLive());
    oldLivePhysicalDatabase.setDriverClassName(newLivePhysicalDatabase.getDriverClassName());
    oldLivePhysicalDatabase.setUrl(newLivePhysicalDatabase.getUrl());
    oldLivePhysicalDatabase.setUsername(newLivePhysicalDatabase.getUsername());
    oldLivePhysicalDatabase.setPassword(newLivePhysicalDatabase.getPassword());

    newLivePhysicalDatabase.setDatabaseType(oldLiveDatabaseType);
    newLivePhysicalDatabase.setInstanceName(oldLiveInstanceName);
    newLivePhysicalDatabase.setLive(oldLiveIsLive);
    newLivePhysicalDatabase.setDriverClassName(oldLiveDriverClassName);
    newLivePhysicalDatabase.setUrl(oldLiveUrl);
    newLivePhysicalDatabase.setUsername(oldLiveUsername);
    newLivePhysicalDatabase.setPassword(oldLivePassword);
  }

  /**
   * Opens a transaction and persists the updated physicaldb entities.
   */
  private void persistModel(boolean noop)
  {
    LOGGER.info(context() + "Swapping physical database links used by stage environment and live environment" + noopRemark(noop));
    if (!noop)
    {
      environmentTx.updateEnvironment(liveEnv); //Cascades to modified physicaldb.
      environmentTx.updateEnvironment(stageEnv); //Cascades to modified physicaldb.
    }
  }
}
