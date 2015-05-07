package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;

/**
 * Associates the stage environment with the live database.
 * <p/>
 * This is just a change to the bluegreen datamodel, it does not affect any running applications.
 */
@Lazy
@Component
public class LinkLiveDatabaseTask extends TwoEnvTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(LinkLiveDatabaseTask.class);

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
    linkOldToNew();
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
   * Changes the newLive physicaldb entity to be the same as oldLive physicaldb.
   * (Everything except the id and logicaldb parent.)
   */
  private void linkOldToNew()
  {
    newLivePhysicalDatabase.setDatabaseType(oldLivePhysicalDatabase.getDatabaseType());
    newLivePhysicalDatabase.setInstanceName(oldLivePhysicalDatabase.getInstanceName());
    newLivePhysicalDatabase.setLive(oldLivePhysicalDatabase.isLive());
    newLivePhysicalDatabase.setDriverClassName(oldLivePhysicalDatabase.getDriverClassName());
    newLivePhysicalDatabase.setUrl(oldLivePhysicalDatabase.getUrl());
    newLivePhysicalDatabase.setUsername(oldLivePhysicalDatabase.getUsername());
    newLivePhysicalDatabase.setPassword(oldLivePhysicalDatabase.getPassword());
  }

  /**
   * Opens a transaction and persists the updated newLive physicaldb.
   */
  private void persistModel(boolean noop)
  {
    LOGGER.info(context(stageEnv) + "Linking stage environment to live physical database" + noopRemark(noop));
    if (!noop)
    {
      environmentTx.updateEnvironment(stageEnv); //Cascades to modified physicaldb.
    }
  }
}
