package com.nike.tools.bgm.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsAnalyzer;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsCopierFactory;
import com.nike.tools.bgm.model.domain.DatabaseType;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.Waiter;
import com.nike.tools.bgm.utils.WaiterParameters;

/**
 * Takes a snapshot of the live RDS instance and restores it in the new staging environment.
 * Also copies the live RDS parameter group so staging can have its own.
 * <p/>
 * Restored copy will be identical in every way except for rds instname and parameter group.
 * We are making an assumption that the live instance has a paramgroup whose name embeds the instname.
 * And an additional assumption that the paramgroup has a read_only parameter.
 * <p/>
 * Pre-existing stage environment is an error, because this is the task that initially creates the stage env.
 */
@Lazy
@Component
public class RdsSnapshotRestoreTask extends TaskImpl
{
  private static final Pattern JDBC_URL = Pattern.compile("(jdbc:mysql://)([^:/]+)(.*)");

  /**
   * Using '9' as a delimiter char, since the only other special char allowed in a snapshotId is '-' and we are
   * already using that inside the tokens we use to make the id.
   */
  private static final char SNAPSHOT_ID_DELIMITER = '9';
  private static final String SNAPSHOT_PREFIX = "bluegreen";

  private static final Logger LOGGER = LoggerFactory.getLogger(RdsSnapshotRestoreTask.class);

  @Autowired
  @Qualifier("rdsSnapshotRestoreTask")
  private WaiterParameters waiterParameters;

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private RdsCopierFactory rdsCopierFactory;

  @Autowired
  private RdsAnalyzer rdsAnalyzer;

  @Autowired
  private ThreadSleeper threadSleeper;

  private String liveEnvName;
  private String stageEnvName;
  private Map<String, String> dbMap; //Maps liveLogicalName to new stagePhysicalInstanceName

  private Environment liveEnv;
  private LogicalDatabase liveLogicalDatabase;
  private PhysicalDatabase livePhysicalDatabase;
  private Environment stageEnv;
  private LogicalDatabase stageLogicalDatabase;
  private PhysicalDatabase stagePhysicalDatabase;
  private RdsClient rdsClient;

  /**
   * @param dbMap Maps live logical dbname to new stage physical dbname.
   */
  public Task assign(int position, String liveEnvName, String stageEnvName, Map<String, String> dbMap)
  {
    if (StringUtils.equals(liveEnvName, stageEnvName))
    {
      throw new IllegalArgumentException("Live env must be different from stage env, cannot target env '" + liveEnvName + "' for both");
    }
    super.assign(position);
    this.liveEnvName = liveEnvName;
    this.stageEnvName = stageEnvName;
    this.dbMap = dbMap;
    return this;
  }

  /**
   * Loads datamodel entities and asserts preconditions on them.  These assertions should be true at the moment when
   * this task is about to begin processing.
   * <p/>
   * Looks up the environment entities by name.
   * Currently requires that the live env has exactly one logicaldb, with one physicaldb.
   * Error if any prior stage database exists.
   */
  protected void loadDataModel()
  {
    this.liveEnv = environmentTx.findNamedEnv(liveEnvName);
    this.liveLogicalDatabase = findLiveLogicalDatabaseFromEnvironment();
    this.livePhysicalDatabase = liveLogicalDatabase.getPhysicalDatabase();
    checkLivePhysicalDatabase();
    checkNoStageEnvironment();
    checkDbMap();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   * Based on Environment objects.
   */
  private String contextFromEnv(String envType, Environment environment,
                                LogicalDatabase logicalDatabase, PhysicalDatabase physicalDatabase)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[" + envType + "Env '" + environment.getEnvName() + "'");
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
   * Returns a string that describes the requested stage env context, for logging purposes.
   * Based on cmdline arguments and existing live env info.
   */
  private String stageContextFromArgs()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[stageEnv '" + stageEnvName + "'");
    if (liveLogicalDatabase != null) //stage logicaldb will get same name as live logicaldb
    {
      sb.append(", ");
      final String liveLogicalName = liveLogicalDatabase.getLogicalName();
      sb.append(liveLogicalName);
      if (dbMap != null)
      {
        final String stagePhysicalInstanceName = dbMap.get(liveLogicalName);
        if (StringUtils.isNotBlank(stagePhysicalInstanceName))
        {
          sb.append(" - RDS ");
          sb.append(stagePhysicalInstanceName);
        }
      }
    }
    sb.append("]: ");
    return sb.toString();
  }

  String liveContext()
  {
    return contextFromEnv("live", liveEnv, liveLogicalDatabase, livePhysicalDatabase);
  }

  String stageContext()
  {
    if (stageEnv != null)
    {
      return contextFromEnv("stage", stageEnv, stageLogicalDatabase, stagePhysicalDatabase);
    }
    else
    {
      return stageContextFromArgs();
    }
  }

  /**
   * Gets the live env's persisted logicaldb record.  Requires exactly 1.
   */
  private LogicalDatabase findLiveLogicalDatabaseFromEnvironment()
  {
    List<LogicalDatabase> logicalDatabases = liveEnv.getLogicalDatabases();
    if (CollectionUtils.isEmpty(logicalDatabases))
    {
      throw new IllegalStateException(liveContext() + "No logical databases");
    }
    else if (logicalDatabases.size() > 1)
    {
      throw new UnsupportedOperationException(liveContext() + "Currently only support case of 1 logicalDatabase, but live env has "
          + logicalDatabases.size() + ": " + listOfNames(logicalDatabases));
    }
    else if (StringUtils.isBlank(logicalDatabases.get(0).getLogicalName()))
    {
      throw new IllegalStateException(liveContext() + "Live logical database has blank name");
    }
    return logicalDatabases.get(0);
  }

  /**
   * Checks that the physicaldb which user specified as live is in fact live and RDS type.
   */
  private void checkLivePhysicalDatabase()
  {
    if (livePhysicalDatabase == null)
    {
      throw new IllegalStateException(liveContext() + "Live logical database is not mapped to a physical database");
    }
    if (!livePhysicalDatabase.isLive())
    {
      throw new IllegalStateException(liveContext() + "Physical database record for this env claims it is not live");
    }
    if (livePhysicalDatabase.getDatabaseType() != DatabaseType.RDS)
    {
      throw new IllegalStateException(liveContext() + "Live physical database is type " + livePhysicalDatabase.getDriverClassName()
          + ", cannot perform RDS snapshot/restore operations on it");
    }
    if (StringUtils.isBlank(livePhysicalDatabase.getInstanceName()))
    {
      throw new IllegalArgumentException(liveContext() + "Live physical database has blank instance name");
    }
  }

  /**
   * Checks that stage env does not exist yet.
   */
  private void checkNoStageEnvironment()
  {
    Environment stageEnv = environmentTx.findNamedEnvAllowNull(stageEnvName);
    if (stageEnv != null)
    {
      throw new IllegalStateException(stageContext() + "Stage env exists already, with "
          + CollectionUtils.size(stageEnv.getLogicalDatabases()) + " logical databases ["
          + listOfNames(stageEnv.getLogicalDatabases())
          + "], you must manually destroy the stage env and run this job again");
    }
  }

  /**
   * Makes a comma-separated list of database names.
   * Uses logical name (with physical instname and url in parentheses if applicable).
   */
  private String listOfNames(List<LogicalDatabase> logicalDatabases)
  {
    StringBuilder sb = new StringBuilder();
    if (logicalDatabases != null)
    {
      for (LogicalDatabase logicalDatabase : logicalDatabases)
      {
        if (sb.length() > 0)
        {
          sb.append(", ");
        }
        sb.append(logicalDatabase.getLogicalName());
        PhysicalDatabase physicalDatabase = logicalDatabase.getPhysicalDatabase();
        if (physicalDatabase != null)
        {
          sb.append(" (");
          sb.append(physicalDatabase.getInstanceName());
          sb.append(" - ");
          sb.append(physicalDatabase.getUrl());
          sb.append(")");
        }
      }
    }
    return sb.toString();
  }

  /**
   * Checks that mapped stage physical instnames are nonblank and are different from live physical instnames.
   * Returns silently if ok.
   * <p/>
   * TODO - expand to support multiple live logicaldbs
   */
  private void checkDbMap()
  {
    final String liveLogicalName = liveLogicalDatabase.getLogicalName();
    if (MapUtils.isEmpty(dbMap) || !dbMap.containsKey(liveLogicalName))
    {
      throw new IllegalArgumentException("Live logical database '" + liveLogicalName
          + "' is unmapped, don't know what stage physical instname to create");
    }
    final String stagePhysicalInstanceName = dbMap.get(liveLogicalName);
    if (StringUtils.isBlank(stagePhysicalInstanceName))
    {
      throw new IllegalArgumentException("You have mapped live logical database '" + liveLogicalName
          + "' to a blank string, we don't know what stage physical instname to create");
    }
    if (StringUtils.equals(stagePhysicalInstanceName, livePhysicalDatabase.getInstanceName()))
    {
      throw new IllegalArgumentException("You have mapped live logical database '" + liveLogicalName
          + "' to stage physical instname '" + stagePhysicalInstanceName
          + "', but live physical database is already using that instname");
    }
  }

  /**
   * Takes a snapshot of the live RDS instance and restores it in the new staging environment.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    rdsClient = rdsCopierFactory.create();
    DBInstance liveInstance = describeLiveInstance();
    DBSnapshot dbSnapshot = snapshotLive(noop);
    DBParameterGroup stageParamGroup = copyParameterGroup(liveInstance, noop);
    DBInstance stageInstance = restoreStage(dbSnapshot, stageParamGroup, liveInstance, noop);
    persistModel(stageInstance, noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  /**
   * Gets current info on the live database physical instance.
   * <p/>
   * Read-only, so it operates regardless of noop setting.
   */
  private DBInstance describeLiveInstance()
  {
    LOGGER.info(liveContext() + "Requesting description of live RDS instance");
    return rdsClient.describeInstance(livePhysicalDatabase.getInstanceName());
  }

  /**
   * Takes a snapshot of the live RDS instance, waits for completion.
   * Sanity-checks the result.
   */
  DBSnapshot snapshotLive(boolean noop)
  {
    LOGGER.info(liveContext() + "Taking snapshot of live RDS instance" + noopRemark(noop));
    DBSnapshot dbSnapshot = null;
    if (!noop)
    {
      String snapshotId = makeSnapshotId();
      dbSnapshot = rdsClient.createSnapshot(snapshotId, livePhysicalDatabase.getInstanceName());
      dbSnapshot = waitTilSnapshotIsAvailable(snapshotId, dbSnapshot);
    }
    return dbSnapshot;
  }

  /**
   * Makes an RDS snapshot id based on the live physicaldb.  Always the same string for a given physicaldb.
   * <p/>
   * If we didn't specify one, Amazon would create a random identifier for us.
   */
  String makeSnapshotId()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(SNAPSHOT_PREFIX);
    sb.append(SNAPSHOT_ID_DELIMITER);
    sb.append(liveEnv.getEnvName());
    sb.append(SNAPSHOT_ID_DELIMITER);
    sb.append(liveLogicalDatabase.getLogicalName());
    sb.append(SNAPSHOT_ID_DELIMITER);
    sb.append(livePhysicalDatabase.getInstanceName());
    return sb.toString();
  }

  /**
   * Creates a Waiter using a snapshot progress checker, and returns the final DBSnapshot when waiting is done.
   * In case of error - never returns null, throws instead.
   */
  private DBSnapshot waitTilSnapshotIsAvailable(String snapshotId, DBSnapshot initialSnapshot)
  {
    LOGGER.info(liveContext() + "Waiting for snapshot to become available");
    RdsSnapshotProgressChecker progressChecker = new RdsSnapshotProgressChecker(snapshotId, liveContext(), rdsClient,
        initialSnapshot);
    Waiter<DBSnapshot> waiter = new Waiter(waiterParameters, threadSleeper, progressChecker);
    DBSnapshot dbSnapshot = waiter.waitTilDone();
    if (dbSnapshot == null)
    {
      throw new RuntimeException(liveContext() + "Snapshot did not become available");
    }
    return dbSnapshot;
  }

  /**
   * Makes a copy of the live instance's parameter group.
   */
  private DBParameterGroup copyParameterGroup(DBInstance liveInstance, boolean noop)
  {
    String stagePhysicalInstanceName = dbMap.get(liveLogicalDatabase.getLogicalName());
    String liveParamGroupName = rdsAnalyzer.findSelfNamedOrDefaultParamGroupName(liveInstance);
    String stageParamGroupName = makeStageParamGroupName(liveParamGroupName,
        liveInstance.getDBInstanceIdentifier(), stagePhysicalInstanceName);
    LOGGER.info(liveContext() + "Copying live parameter group '" + liveParamGroupName
        + "' to stage parameter group '" + stageParamGroupName + "'" + noopRemark(noop));
    if (!noop)
    {
      return rdsClient.copyParameterGroup(liveParamGroupName, stageParamGroupName);
    }
    else
    {
      return null;
    }
  }

  /**
   * Makes the name for a new stage paramGroup, based on replacing the live part with a stage part.
   * <p/>
   * e.g. Changes "the-paramgroup-livename" to "the-paramgroup-stagename".
   * <p/>
   * In case the current live paramGroupName doesn't contain the live instname, then simply appends the
   * stage instname instead of trying to replace.
   */
  private String makeStageParamGroupName(String liveParamGroupName,
                                         String livePhysicalInstanceName, String stagePhysicalInstanceName)
  {
    if (StringUtils.contains(liveParamGroupName, livePhysicalInstanceName))
    {
      return liveParamGroupName.replace(livePhysicalInstanceName, stagePhysicalInstanceName);
    }
    else
    {
      return liveParamGroupName + "-" + stagePhysicalInstanceName;
    }
  }

  /**
   * Restores the live snapshot into the new staging environment.
   * Then makes a few small modifications that restore would not do automatically.
   * Returns the modified instance.
   */
  DBInstance restoreStage(DBSnapshot dbSnapshot,
                          DBParameterGroup stageParamGroup,
                          DBInstance liveInstance,
                          boolean noop)
  {
    LOGGER.info(liveContext() + "Restoring snapshot to new stage RDS instance" + noopRemark(noop));
    if (!noop)
    {
      String stagePhysicalInstanceName = dbMap.get(liveLogicalDatabase.getLogicalName());
      initModel(stagePhysicalInstanceName);
      String subnetGroupName = getSubnetGroupName(liveInstance);
      DBInstance stageInstance = rdsClient.restoreInstanceFromSnapshot(stagePhysicalInstanceName,
          dbSnapshot.getDBSnapshotIdentifier(), subnetGroupName);
      stageInstance = waitTilInstanceIsAvailable(stagePhysicalInstanceName, stageInstance, true/*create*/);
      DBInstance modifiedInstance = modifyInstance(stageInstance, stageParamGroup, liveInstance);
      modifiedInstance = waitTilInstanceIsAvailable(stagePhysicalInstanceName, modifiedInstance, false/*modify*/);
      return modifiedInstance;
    }
    return null;
  }

  /**
   * Returns the instance's subnet group name, or null if none.
   */
  private String getSubnetGroupName(DBInstance dbInstance)
  {
    if (dbInstance != null && dbInstance.getDBSubnetGroup() != null)
    {
      return dbInstance.getDBSubnetGroup().getDBSubnetGroupName();
    }
    return null;
  }

  /**
   * Creates a Waiter using an instance progress checker, and returns the final DBInstance when waiting is done.
   * In case of error - never returns null, throws instead.
   *
   * @param create True if we're waiting on Create Instance, false if Modify Instance.
   */
  private DBInstance waitTilInstanceIsAvailable(String instanceId, DBInstance initialInstance, boolean create)
  {
    LOGGER.info(liveContext() + "Waiting for instance to become available");
    RdsInstanceProgressChecker progressChecker = new RdsInstanceProgressChecker(instanceId, liveContext(), rdsClient,
        initialInstance, create);
    Waiter<DBInstance> waiter = new Waiter(waiterParameters, threadSleeper, progressChecker);
    DBInstance dbInstance = waiter.waitTilDone();
    if (dbInstance == null)
    {
      throw new RuntimeException(liveContext() + progressChecker.getDescription() + " did not become available");
    }
    return dbInstance;
  }

  /**
   * Modifies the stage instance to use the same security groups as liveInstance, and a new paramgroup.
   */
  private DBInstance modifyInstance(DBInstance stageInstance, DBParameterGroup stageParamGroup, DBInstance liveInstance)
  {
    Collection<String> vpcSecurityGroupIds = rdsAnalyzer.extractVpcSecurityGroupIds(liveInstance);
    return rdsClient.modifyInstanceWithSecgrpParamgrp(
        stageInstance.getDBInstanceIdentifier(),
        vpcSecurityGroupIds,
        stageParamGroup.getDBParameterGroupName());
  }

  /**
   * Initializes transient entities for the new stage database.
   */
  void initModel(String stagePhysicalInstanceName)
  {
    stageEnv = makeStageEnvironmentEntity();
    stageLogicalDatabase = makeStageLogicalDatabaseEntity(liveLogicalDatabase.getLogicalName());
    stagePhysicalDatabase = makeStagePhysicalDatabaseEntity(stagePhysicalInstanceName);
    stageLogicalDatabase.setPhysicalDatabase(stagePhysicalDatabase);
    stagePhysicalDatabase.setLogicalDatabase(stageLogicalDatabase);
  }

  /**
   * Makes a transient entity for the new stage environment.
   */
  private Environment makeStageEnvironmentEntity()
  {
    Environment stageEnv = new Environment();
    stageEnv.setEnvName(stageEnvName);
    return stageEnv;
  }

  /**
   * Makes a transient entity for the new stage logicaldb.  Refers to stage env but no applicationVm yet.
   */
  private LogicalDatabase makeStageLogicalDatabaseEntity(String stageLogicalName)
  {
    LogicalDatabase stageLogicalDatabase = new LogicalDatabase();
    stageLogicalDatabase.setEnvironment(stageEnv);
    stageLogicalDatabase.setLogicalName(stageLogicalName);
    stageEnv.addLogicalDatabase(stageLogicalDatabase);
    return stageLogicalDatabase;
  }

  /**
   * Makes a transient entity for the new stage physicaldb.
   * <p/>
   * Stage physical info is mostly copied directly from live physical info, but the following fields are different:
   * live (false), instname, url.
   * <p/>
   * Physical url is not known yet.
   */
  private PhysicalDatabase makeStagePhysicalDatabaseEntity(String stagePhysicalInstanceName)
  {
    PhysicalDatabase stagePhysicalDatabase = new PhysicalDatabase();
    stagePhysicalDatabase.setDatabaseType(livePhysicalDatabase.getDatabaseType());
    stagePhysicalDatabase.setInstanceName(stagePhysicalInstanceName);
    stagePhysicalDatabase.setDriverClassName(livePhysicalDatabase.getDriverClassName());
    stagePhysicalDatabase.setUsername(livePhysicalDatabase.getUsername());
    stagePhysicalDatabase.setPassword(livePhysicalDatabase.getPassword());
    return stagePhysicalDatabase;
  }

  /**
   * Sets the stage physical url, then opens a transaction to insert bluegreen records for the new stage environment.
   */
  private void persistModel(DBInstance stageInstance, boolean noop)
  {
    if (!noop)
    {
      String stagePhysicalUrl = makeStagePhysicalUrl(livePhysicalDatabase.getUrl(), stageInstance.getEndpoint().getAddress());
      stagePhysicalDatabase.setUrl(stagePhysicalUrl);
      environmentTx.newEnvironment(stageEnv); //Cascades to new stage physicaldb.
    }
  }

  /**
   * Makes a JDBC url for the stage physical database, which should be the same as the live physical url except for
   * the endpoint address.
   * <p/>
   * e.g. change 'jdbc:mysql://live.hello.com:3306/dbname' to 'jdbc:mysql://stage.hello.com:3306/dbname'.
   * <p/>
   * Live url might use an ELB or pretty CNAME instead of a private aws endpoint address.  However the stage url
   * will only use the endpoint address.
   */
  String makeStagePhysicalUrl(String livePhysicalUrl, String stagePhysicalAddress)
  {
    if (StringUtils.isBlank(livePhysicalUrl))
    {
      throw new IllegalStateException(liveContext() + "Lost live physical url");
    }
    if (StringUtils.isBlank(stagePhysicalAddress))
    {
      throw new IllegalArgumentException(stageContext() + "RDS instance missing endpoint address");
    }
    Matcher matcher = JDBC_URL.matcher(livePhysicalUrl);
    if (!matcher.matches())
    {
      throw new RuntimeException(liveContext() + "Don't know how to replace endpoint in live physical url '"
          + livePhysicalUrl + "'");
    }
    return matcher.group(1) + stagePhysicalAddress + matcher.group(3);
  }

  //Test purposes only
  LogicalDatabase getStageLogicalDatabase()
  {
    return stageLogicalDatabase;
  }

  //Test purposes only
  PhysicalDatabase getStagePhysicalDatabase()
  {
    return stagePhysicalDatabase;
  }

}
