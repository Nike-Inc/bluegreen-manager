package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsAnalyzer;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsClientFactory;
import com.nike.tools.bgm.client.aws.RdsInstanceStatus;
import com.nike.tools.bgm.client.aws.RdsSnapshotBluegreenId;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.OneEnvLoader;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.Waiter;
import com.nike.tools.bgm.utils.WaiterParameters;

/**
 * Deletes the target RDS instance, its parameter group (if non-default), and the bluegreen snapshot from which it was
 * originally made.
 * <p/>
 * Deletes the snapshot prefixed with 'bluegreen' since the next stagingDeploy will need to create another one with
 * exactly the same name.  However this task leaves behind any snapshots that Amazon automatically made of the database
 * we're deleting, and they will persist until manually deleted.
 * <p/>
 * Only deletes the parameter group when it is clear that stagingDeploy created it specifically for the RDS instance
 * that we're deleting.
 * <p/>
 * Automated deletion is DANGEROUS.  This task needs to be extremely well maintained and tested.  Our only safety net
 * is the "isLive" flag: we only delete non-live instances.
 */
public class RdsInstanceDeleteTask extends TaskImpl
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsInstanceDeleteTask.class);

  @Autowired
  @Qualifier("rdsInstanceDeleteTask")
  private WaiterParameters waiterParameters;

  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  @Autowired
  private RdsClientFactory rdsClientFactory;

  @Autowired
  private RdsAnalyzer rdsAnalyzer;

  @Autowired
  private ThreadSleeper threadSleeper;

  private OneEnvLoader oneEnvLoader;
  private RdsClient rdsClient;

  private String envName;

  private Environment environment;
  private LogicalDatabase logicalDatabase;
  private PhysicalDatabase physicalDatabase;

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
   * Currently requires that the target env has exactly one logicaldb, with one physicaldb.
   */
  private void loadDataModel()
  {
    this.oneEnvLoader = envLoaderFactory.createOne(envName);
    oneEnvLoader.loadPhysicalDatabase();
    this.environment = oneEnvLoader.getEnvironment();
    this.logicalDatabase = oneEnvLoader.getLogicalDatabase();
    this.physicalDatabase = oneEnvLoader.getPhysicalDatabase();
  }

  private String context()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[Environment '" + environment.getEnvName() + "'");
    if (logicalDatabase != null)
    {
      sb.append(", ");
      sb.append(logicalDatabase.getLogicalName());
      if (StringUtils.isNotBlank(physicalDatabase.getInstanceName()))
      {
        sb.append(" - RDS ");
        sb.append(physicalDatabase.getInstanceName());
      }
    }
    sb.append("]: ");
    return sb.toString();
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
    checkDatabaseIsNotLive();
    rdsClient = rdsClientFactory.create();
    DBInstance rdsInstance = describeInstance();
    String paramGroupName = rdsAnalyzer.findSelfNamedParamGroupName(rdsInstance);
    deleteInstance(noop);
    deleteParameterGroup(paramGroupName, noop);
    deleteSnapshot(noop);
    return null;
  }

  /**
   * Performs the single most important check of this task: asserts that the target database is not live.
   * <p/>
   * It would be Very Very Bad to delete a live database!
   */
  private void checkDatabaseIsNotLive()
  {
    if (physicalDatabase.isLive())
    {
      throw new IllegalArgumentException("Are you CRAZY??? Don't ask us to delete a LIVE database!!!");
    }
  }

  /**
   * Gets current info on the live database physical instance.
   * <p/>
   * Read-only, so it operates regardless of noop setting.
   */
  private DBInstance describeInstance()
  {
    LOGGER.info(context() + "Requesting description of target RDS instance");
    return rdsClient.describeInstance(physicalDatabase.getInstanceName());
  }

  /**
   * Requests deletion of the target RDS instance, does not wait for confirmed deletion.
   */
  private void deleteInstance(boolean noop)
  {
    LOGGER.info(context() + "Deleting non-live target RDS instance" + noopRemark(noop));
    if (!noop)
    {
      DBInstance initialInstance = rdsClient.deleteInstance(physicalDatabase.getInstanceName());
      waitTilInstanceIsDeleted(initialInstance);
    }
  }

  /**
   * Creates a Waiter and returns when the instance is fully deleted.
   */
  private void waitTilInstanceIsDeleted(DBInstance initialInstance)
  {
    LOGGER.info(context() + "Waiting for instance to be deleted");
    RdsInstanceProgressChecker progressChecker = new RdsInstanceProgressChecker(initialInstance.getDBInstanceIdentifier(),
        context(), rdsClient, initialInstance, RdsInstanceStatus.DELETING);
    Waiter<DBInstance> waiter = new Waiter(waiterParameters, threadSleeper, progressChecker);
    DBInstance dbInstance = waiter.waitTilDone();
    if (dbInstance == null)
    {
      throw new RuntimeException(context() + progressChecker.getDescription() + " was not deleted");
    }
  }

  /**
   * Deletes the parameter group, if it appears to have been created solely for the deleted db instance.
   * <p/>
   * (The parameter group cannot be deleted until the dependent rdsInstance is fully deleted.)
   */
  private void deleteParameterGroup(String paramGroupName, boolean noop)
  {
    if (StringUtils.isBlank(paramGroupName))
    {
      LOGGER.info(context() + "Deleted database did have its own special parameter group");
    }
    else
    {
      LOGGER.info(context() + "Deleting parameter group '" + paramGroupName + "', was used only by the deleted database");
      if (!noop)
      {
        rdsClient.deleteParameterGroup(paramGroupName);
      }
    }
  }

  /**
   * Deletes the bluegreen snapshot from which the deleted database instance was originally made.
   * <p/>
   * Does not delete snapshots that Amazon may have made automatically.
   */
  private void deleteSnapshot(boolean noop)
  {
    LOGGER.info(context() + "Deleting snapshot from which the deleted database was originally made");
    if (!noop)
    {
      String snapshotId = makeSnapshotId();
      DBSnapshot initialSnapshot = rdsClient.deleteSnapshot(snapshotId);
      /*
      TODO - wait for confirmation that snapshot is deleted.
      Unlike rds instances, snapshots have 'deleting' but not 'deleted' status, so confirmation would come from
      describe-snapshots throwing exception ("not found"), or getting the list of all snapshots and seeing that
      the delete target is not in the list anymore.
       */
    }
  }

  private String makeSnapshotId()
  {
    RdsSnapshotBluegreenId id = new RdsSnapshotBluegreenId(envName, logicalDatabase.getLogicalName(),
        physicalDatabase.getInstanceName());
    return id.toString();
  }

}
