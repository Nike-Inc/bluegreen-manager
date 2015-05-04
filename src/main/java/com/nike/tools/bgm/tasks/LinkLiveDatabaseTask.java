package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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
   * Changes the newLive physicaldb entity to be the same as oldLive physicaldb.
   * (Everything except the id and logicaldb parent.)
   */
  private void linkOldToNew()
  {
    newLivePhysicalDatabase.setDatabaseType(oldLivePhysicalDatabase.getDatabaseType());
    newLivePhysicalDatabase.setInstanceName(oldLivePhysicalDatabase.getInstanceName());
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
