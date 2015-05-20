package com.nike.tools.bgm.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBParameterGroupStatus;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DBSnapshotNotFoundException;
import com.amazonaws.services.rds.model.DBSubnetGroup;
import com.amazonaws.services.rds.model.Endpoint;
import com.nike.tools.bgm.client.aws.RdsAnalyzer;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsClientFactory;
import com.nike.tools.bgm.client.aws.RdsInstanceStatus;
import com.nike.tools.bgm.client.aws.RdsParameterApplyStatus;
import com.nike.tools.bgm.client.aws.RdsSnapshotStatus;
import com.nike.tools.bgm.model.domain.DatabaseTestHelper;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.WaiterParameters;

import static com.nike.tools.bgm.model.domain.DatabaseTestHelper.LIVE_ENV_NAME;
import static com.nike.tools.bgm.model.domain.DatabaseTestHelper.LIVE_LOGICAL_NAME;
import static com.nike.tools.bgm.model.domain.DatabaseTestHelper.LIVE_PHYSICAL_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * TODO - The high level restoreStage tests are too complicated...would help to modularize RDSSnapshotRestoreTask better, then tests would become simpler
 */
@RunWith(MockitoJUnitRunner.class)
public class RdsSnapshotRestoreTaskTest
{
  private static final PhysicalDatabase FAKE_PHYSICAL_DATABASE = DatabaseTestHelper.makeFakeLiveDatabase();
  private static final String STAGE_ENV_NAME = "stageEnv";
  private static final String STAGE_PHYSICAL_NAME = "spdata";
  private static final String FAKE_SNAPSHOT_ID = "the-snapshot-123";
  private static final String LIVE_PARAM_GROUP_NAME = "live-param-group";
  private static final String SIMPLE_STAGE_PARAM_GROUP_NAME = "stage-param-group";
  private static final String STAGE_ENDPOINT_ADDRESS = "stage.hello.com";
  private static final String SUBNET_GROUP = "bigvpcsubnet";
  private static final String UGLY_STAGE_PARAM_GROUP_NAME = LIVE_PARAM_GROUP_NAME + "-" + STAGE_PHYSICAL_NAME;
  private final Environment fakeStageEnv = new Environment() //non-static: needs to be reinitialized on every test
  {{
      setEnvName(STAGE_ENV_NAME);
    }};
  private static final Map<String, String> DB_MAP = new HashMap<String, String>()
  {{
      put(LIVE_LOGICAL_NAME, STAGE_PHYSICAL_NAME);
    }};

  @InjectMocks
  private RdsSnapshotRestoreTask rdsSnapshotRestoreTask;

  @Spy
  protected WaiterParameters fakeWaiterParameters = new WaiterParameters(10L, 10L, 2, 20);

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Mock
  private RdsClientFactory mockRdsClientFactory;

  @Spy //Would be nice to Mock instead of Spy, but would need more when().thenReturn() in restoreSetup
  private RdsAnalyzer mockRdsAnalyzer;

  @Mock
  private RdsClient mockRdsClient;

  @Mock
  private ThreadSleeper mockThreadSleeper;

  /**
   * Initializes the object-under-test for the "normal" case where live/stage envs meet preconditions.
   */
  private void normalSetup()
  {
    when(mockEnvironmentTx.findNamedEnv(LIVE_ENV_NAME)).thenReturn(FAKE_PHYSICAL_DATABASE.getLogicalDatabase().getEnvironment());
    when(mockEnvironmentTx.findNamedEnv(STAGE_ENV_NAME)).thenReturn(null);
    when(mockRdsClientFactory.create()).thenReturn(mockRdsClient);
    rdsSnapshotRestoreTask.assign(1, LIVE_ENV_NAME, STAGE_ENV_NAME, DB_MAP);
    rdsSnapshotRestoreTask.loadDataModel();
  }

  //TODO - test loadDataModel() WITHOUT normalSetup() ...i.e. error cases

  /**
   * Live context after assign should show env/logical/physical.
   */
  @Test
  public void testLiveContext()
  {
    normalSetup();
    String context = rdsSnapshotRestoreTask.liveContext();
    assertTrue(context.contains(LIVE_ENV_NAME));
    assertTrue(context.contains(LIVE_LOGICAL_NAME));
    assertTrue(context.contains(LIVE_PHYSICAL_NAME));
  }

  /**
   * Stage context after assign should show env/logical/physical using stageContextFromArgs.
   */
  @Test
  public void testStageContext()
  {
    normalSetup();
    String context = rdsSnapshotRestoreTask.stageContext();
    assertTrue(context.contains(STAGE_ENV_NAME));
    assertTrue(context.contains(LIVE_LOGICAL_NAME)); //Stage logical is taken from live logical name.
    assertTrue(context.contains(STAGE_PHYSICAL_NAME));
  }

  /**
   * Prior snapshot exists, and is seen to be deleted on the first checker followup.
   */
  @Test
  public void testDeletePriorLiveSnapshot_Exists()
  {
    normalSetup();
    DBSnapshot dbSnapshot = makeFakeSnapshot(RdsSnapshotStatus.DELETING);
    when(mockRdsClient.describeSnapshot(anyString())).thenReturn(dbSnapshot).thenThrow(DBSnapshotNotFoundException.class);
    when(mockRdsClient.deleteSnapshot(anyString())).thenReturn(dbSnapshot);

    rdsSnapshotRestoreTask.deletePriorLiveSnapshot(false/*noop*/);

    verify(mockRdsClient, times(2)).describeSnapshot(anyString());
    verify(mockRdsClient).deleteSnapshot(anyString());
  }

  /**
   * No prior snapshot exists.
   */
  @Test
  public void testDeletePriorLiveSnapshot_DoesNotExist()
  {
    normalSetup();
    when(mockRdsClient.describeSnapshot(anyString())).thenThrow(DBSnapshotNotFoundException.class);

    rdsSnapshotRestoreTask.deletePriorLiveSnapshot(false/*noop*/);

    verify(mockRdsClient).describeSnapshot(anyString());
    verify(mockRdsClient, never()).deleteSnapshot(anyString());
  }

  /**
   * Tests the case where the snapshot is created with an unexpected id.
   */
  @Test(expected = IllegalStateException.class)
  public void testSnapshotLive_BadId()
  {
    normalSetup();
    DBSnapshot dbSnapshot = new DBSnapshot();
    dbSnapshot.setDBSnapshotIdentifier("The Wrong ID");
    when(mockRdsClient.createSnapshot(anyString(), anyString())).thenReturn(dbSnapshot);

    rdsSnapshotRestoreTask.snapshotLive(false/*noop*/);
  }

  private DBSnapshot makeFakeSnapshot(RdsSnapshotStatus status)
  {
    DBSnapshot dbSnapshot = new DBSnapshot();
    dbSnapshot.setDBSnapshotIdentifier(rdsSnapshotRestoreTask.makeSnapshotId());
    dbSnapshot.setStatus(status.toString());
    return dbSnapshot;
  }

  /**
   * Tests the case where the snapshot is created with an unexpected status.
   */
  @Test
  public void testSnapshotLive_Pass()
  {
    normalSetup();
    DBSnapshot dbSnapshot = makeFakeSnapshot(RdsSnapshotStatus.AVAILABLE);
    when(mockRdsClient.createSnapshot(anyString(), anyString())).thenReturn(dbSnapshot);

    assertEquals(dbSnapshot, rdsSnapshotRestoreTask.snapshotLive(false/*noop*/));
  }

  /**
   * Results of testing restoreStage, including original fake data.
   */
  private static class RestoreStageResults
  {
    private RestoreStageFakeData data;
    private DBInstance resultInstance;
    private Throwable exception;

    private RestoreStageResults(RestoreStageFakeData data, DBInstance resultInstance, Throwable exception)
    {
      this.data = data;
      this.resultInstance = resultInstance;
      this.exception = exception;
    }

    public RestoreStageFakeData getData()
    {
      return data;
    }

    public DBInstance getResultInstance()
    {
      return resultInstance;
    }

    public Throwable getException()
    {
      return exception;
    }
  }

  /**
   * Fake data for use with testing restoreStage.
   * <p/>
   * TODO - Separate test helper.
   */
  private static class RestoreStageFakeData
  {
    private static final String UNKNOWN_STATUS = "unknown";
    private DBSnapshot dbSnapshot = new DBSnapshot();
    private DBSnapshot priorSnapshot = new DBSnapshot();
    private DBParameterGroup stageParamGroup = new DBParameterGroup();
    private DBInstance liveInstance = makeInstance(LIVE_PHYSICAL_NAME, RdsInstanceStatus.AVAILABLE);
    private DBInstance stageRestoreInstance;
    private DBInstance stageModifyInstance;
    private Collection<String> securityGroups = Arrays.asList("security-group-1");

    public RestoreStageFakeData(String snapshotId,
                                String stageParamGroupName,
                                RdsInstanceStatus stageRestoreInstanceStatus,
                                RdsInstanceStatus stageModifyInstanceStatus,
                                RdsParameterApplyStatus stageModifyParamStatus)
    {
      dbSnapshot.setDBSnapshotIdentifier(snapshotId);
      dbSnapshot.setStatus(RdsSnapshotStatus.AVAILABLE.toString());
      priorSnapshot.setDBSnapshotIdentifier(snapshotId);
      priorSnapshot.setStatus(RdsSnapshotStatus.DELETING.toString());
      stageParamGroup.setDBParameterGroupName(stageParamGroupName);
      stageRestoreInstance = makeInstance(STAGE_PHYSICAL_NAME, stageRestoreInstanceStatus);
      stageModifyInstance = makeInstance(STAGE_PHYSICAL_NAME, stageModifyInstanceStatus, stageParamGroupName, stageModifyParamStatus);
    }

    private DBInstance makeInstance(String instanceName, RdsInstanceStatus instanceStatus)
    {
      //TODO - Refactor to share with RdsInstanceParamGroupProgressCheckerTest#fakeInstance or RdsAnalyzerTest#makeDBInstanceWithParamGroups
      DBInstance dbInstance = new DBInstance();
      dbInstance.setDBInstanceIdentifier(instanceName);
      dbInstance.setDBInstanceStatus(instanceStatus == null ? UNKNOWN_STATUS : instanceStatus.toString());
      Endpoint endpoint = new Endpoint();
      endpoint.setAddress(STAGE_ENDPOINT_ADDRESS);
      dbInstance.setEndpoint(endpoint);
      DBSubnetGroup dbSubnetGroup = new DBSubnetGroup();
      dbSubnetGroup.setDBSubnetGroupName(SUBNET_GROUP);
      dbInstance.setDBSubnetGroup(dbSubnetGroup);
      return dbInstance;
    }

    private DBInstance makeInstance(String instanceName, RdsInstanceStatus instanceStatus,
                                    String paramGroupName, RdsParameterApplyStatus parameterApplyStatus)
    {
      DBInstance dbInstance = makeInstance(instanceName, instanceStatus);
      if (paramGroupName != null)
      {
        List<DBParameterGroupStatus> list = new ArrayList<DBParameterGroupStatus>();
        DBParameterGroupStatus paramGroup = new DBParameterGroupStatus();
        paramGroup.setDBParameterGroupName(paramGroupName);
        paramGroup.setParameterApplyStatus(parameterApplyStatus.toString());
        list.add(paramGroup);
        dbInstance.setDBParameterGroups(list);
      }
      return dbInstance;
    }

    public DBSnapshot getDbSnapshot()
    {
      return dbSnapshot;
    }

    public DBSnapshot getPriorSnapshot()
    {
      return priorSnapshot;
    }

    public DBParameterGroup getStageParamGroup()
    {
      return stageParamGroup;
    }

    public DBInstance getLiveInstance()
    {
      return liveInstance;
    }

    public DBInstance getStageRestoreInstance()
    {
      return stageRestoreInstance;
    }

    public DBInstance getStageModifyInstance()
    {
      return stageModifyInstance;
    }

    public Collection<String> getSecurityGroups()
    {
      return securityGroups;
    }
  }

  /**
   * Sets up fake data and mocks for restoreStage.
   */
  private RestoreStageFakeData restoreSetup(String snapshotId,
                                            String stageParamGroupName,
                                            RdsInstanceStatus stageRestoreInstanceStatus,
                                            RdsInstanceStatus stageModifyInstanceStatus,
                                            RdsParameterApplyStatus stageModifyParamStatus)
  {
    RestoreStageFakeData data = new RestoreStageFakeData(snapshotId, stageParamGroupName,
        stageRestoreInstanceStatus, stageModifyInstanceStatus, stageModifyParamStatus);
    when(mockRdsClient.restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, snapshotId, SUBNET_GROUP))
        .thenReturn(data.getStageRestoreInstance());
    when(mockRdsAnalyzer.extractVpcSecurityGroupIds(data.getLiveInstance())).thenReturn(data.getSecurityGroups());
    when(mockRdsClient.modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), stageParamGroupName))
        .thenReturn(data.getStageModifyInstance());
    when(mockRdsClient.rebootInstance(STAGE_PHYSICAL_NAME)).thenReturn(data.getStageModifyInstance());
    return data;
  }

  /**
   * Test setup and execution for restoreStage.
   *
   * @param stageRestoreInstanceStatus Instance status of stage physicaldb after restore-from-snapshot operation.
   * @param stageModifyInstanceStatus  Instance status of stage physicaldb after modify operation.
   */
  private RestoreStageResults testRestoreStage(String stageParamGroupName,
                                               RdsInstanceStatus stageRestoreInstanceStatus,
                                               RdsInstanceStatus stageModifyInstanceStatus,
                                               RdsParameterApplyStatus stageModifyParamStatus)
  {
    normalSetup();
    RestoreStageFakeData data = restoreSetup(FAKE_SNAPSHOT_ID, stageParamGroupName,
        stageRestoreInstanceStatus, stageModifyInstanceStatus, stageModifyParamStatus);
    DBInstance resultInstance = null;
    Throwable exception = null;
    try
    {
      resultInstance = rdsSnapshotRestoreTask.restoreStage(data.getDbSnapshot(), data.getStageParamGroup(),
          data.getLiveInstance(), false/*noop*/);
    }
    catch (Throwable e)
    {
      exception = e;
    }
    return new RestoreStageResults(data, resultInstance, exception);
  }

  /**
   * Pass case: success restoring snapshot to stage instance, then modifying stage instance.
   */
  @Test
  public void testRestoreStage_Pass()
  {
    RestoreStageResults results = testRestoreStage(SIMPLE_STAGE_PARAM_GROUP_NAME,
        RdsInstanceStatus.AVAILABLE, RdsInstanceStatus.AVAILABLE, RdsParameterApplyStatus.PENDING_REBOOT);
    RestoreStageFakeData data = results.getData();

    assertEquals(data.getStageModifyInstance(), results.getResultInstance());
    assertNull(results.getException());
    verify(mockRdsClient).restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, FAKE_SNAPSHOT_ID, SUBNET_GROUP);
    verify(mockRdsClient).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), SIMPLE_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Fail case: stage instance is restored into unknown status, should throw and not modify.
   */
  @Test
  public void testRestoreStage_FailRestore()
  {
    RestoreStageResults results = testRestoreStage(SIMPLE_STAGE_PARAM_GROUP_NAME,
        null, RdsInstanceStatus.AVAILABLE, RdsParameterApplyStatus.PENDING_REBOOT);
    RestoreStageFakeData data = results.getData();

    assertNull(results.getResultInstance());
    assertEquals(RuntimeException.class, results.getException().getClass());
    verify(mockRdsClient).restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, FAKE_SNAPSHOT_ID, SUBNET_GROUP);
    verify(mockRdsClient, times(0)).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), SIMPLE_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Fail case: stage instance is restored ok but after modify is in unknown status, should throw.
   */
  @Test
  public void testRestoreStage_FailModify()
  {
    RestoreStageResults results = testRestoreStage(SIMPLE_STAGE_PARAM_GROUP_NAME,
        RdsInstanceStatus.AVAILABLE, null, RdsParameterApplyStatus.PENDING_REBOOT);
    RestoreStageFakeData data = results.getData();

    assertNull(results.getResultInstance());
    assertEquals(RuntimeException.class, results.getException().getClass());
    verify(mockRdsClient).restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, FAKE_SNAPSHOT_ID, SUBNET_GROUP);
    verify(mockRdsClient).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), SIMPLE_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Tests that we can assign transient database entities for stage logical and physical.
   * <p/>
   * Physical should have everything but url.
   */
  @Test
  public void testInitModel()
  {
    normalSetup();
    rdsSnapshotRestoreTask.initModel(STAGE_PHYSICAL_NAME);
    LogicalDatabase stageLogicalDatabase = rdsSnapshotRestoreTask.getStageLogicalDatabase();
    PhysicalDatabase stagePhysicalDatabase = rdsSnapshotRestoreTask.getStagePhysicalDatabase();
    assertEquals(fakeStageEnv, stageLogicalDatabase.getEnvironment());
    assertEquals(LIVE_LOGICAL_NAME, stageLogicalDatabase.getLogicalName());
    assertEquals(STAGE_PHYSICAL_NAME, stagePhysicalDatabase.getInstanceName());
  }

  /**
   * Fail case: no live physical url.
   */
  @Test(expected = RuntimeException.class)
  public void testMakeStagePhysicalUrl_NoLivePhysicalUrl()
  {
    rdsSnapshotRestoreTask.makeStagePhysicalUrl("", "stage.hello.com");
  }

  /**
   * Fail case: no stage physical address.
   */
  @Test(expected = RuntimeException.class)
  public void testMakeStagePhysicalUrl_NoStagePhysicalAddress()
  {
    rdsSnapshotRestoreTask.makeStagePhysicalUrl(FAKE_PHYSICAL_DATABASE.getUrl(), null);
  }

  /**
   * Fail case: badly formatted live jdbc url.
   */
  @Test(expected = RuntimeException.class)
  public void testMakeStagePhysicalUrl_BadUrlFormat()
  {
    final String jdbcUrl = "live.hello.com:3306"; //should have 'jdbc:mysql://' prefix and dbname suffix
    final String address = "stage.hello.com";
    rdsSnapshotRestoreTask.makeStagePhysicalUrl(jdbcUrl, address);
  }

  /**
   * Test that we can change 'jdbc:mysql://live.hello.com:3306/dbname' to 'jdbc:mysql://stage.hello.com:3306/dbname'.
   */
  @Test
  public void testMakeStagePhysicalUrl_Pass()
  {
    final String address = "stage.hello.com";
    String stagePhysicalUrl = rdsSnapshotRestoreTask.makeStagePhysicalUrl(FAKE_PHYSICAL_DATABASE.getUrl(), address);
    assertEquals("jdbc:mysql://stage.hello.com:3306/hellodb?zeroDateTimeBehavior=convertToNull", stagePhysicalUrl);
  }

  /**
   * Results of testing process(), including original fake data.
   */
  private static class ProcessResults
  {
    private RestoreStageFakeData data;
    private TaskStatus taskStatus;
    private Throwable exception;

    private ProcessResults(RestoreStageFakeData data, TaskStatus taskStatus, Throwable exception)
    {
      this.data = data;
      this.taskStatus = taskStatus;
      this.exception = exception;
    }

    public RestoreStageFakeData getData()
    {
      return data;
    }

    public TaskStatus getTaskStatus()
    {
      return taskStatus;
    }

    public Throwable getException()
    {
      return exception;
    }
  }

  /**
   * Test setup and execution for the process() method.
   *
   * @param stageRestoreInstanceStatus Instance status of stage physicaldb after restore-from-snapshot operation.
   * @param stageModifyInstanceStatus  Instance status of stage physicaldb after modify operation.
   */
  private ProcessResults testProcess(RdsInstanceStatus stageRestoreInstanceStatus,
                                     RdsInstanceStatus stageModifyInstanceStatus,
                                     RdsParameterApplyStatus stageModifyParamStatus,
                                     boolean noop)
  {
    normalSetup();
    String snapshotId = rdsSnapshotRestoreTask.makeSnapshotId();
    RestoreStageFakeData data = restoreSetup(snapshotId, UGLY_STAGE_PARAM_GROUP_NAME,
        stageRestoreInstanceStatus, stageModifyInstanceStatus, stageModifyParamStatus);
    when(mockRdsClient.describeInstance(LIVE_PHYSICAL_NAME)).thenReturn(data.getLiveInstance());
    when(mockRdsClient.describeSnapshot(anyString())).thenReturn(data.getPriorSnapshot())
        .thenThrow(DBSnapshotNotFoundException.class).thenReturn(data.getDbSnapshot());
    when(mockRdsClient.deleteSnapshot(anyString())).thenReturn(data.getPriorSnapshot());
    when(mockRdsClient.createSnapshot(snapshotId, LIVE_PHYSICAL_NAME)).thenReturn(data.getDbSnapshot());
    when(mockRdsAnalyzer.findSelfNamedOrDefaultParamGroupName(data.getLiveInstance())).thenReturn(LIVE_PARAM_GROUP_NAME);
    when(mockRdsClient.copyParameterGroup(LIVE_PARAM_GROUP_NAME, UGLY_STAGE_PARAM_GROUP_NAME)).thenReturn(data.getStageParamGroup());
    TaskStatus taskStatus = null;
    Throwable exception = null;
    try
    {
      taskStatus = rdsSnapshotRestoreTask.process(noop);
    }
    catch (Throwable e)
    {
      exception = e;
    }
    return new ProcessResults(data, taskStatus, exception);
  }

  private void assertNoException(Throwable exception) throws Throwable
  {
    if (exception != null)
    {
      throw exception;
    }
  }

  /**
   * Pass case: every call to RDS is successful.
   */
  @Test
  public void testProcess_Pass() throws Throwable
  {
    ProcessResults results = testProcess(RdsInstanceStatus.AVAILABLE, RdsInstanceStatus.AVAILABLE,
        RdsParameterApplyStatus.PENDING_REBOOT, false);
    RestoreStageFakeData data = results.getData();

    assertNoException(results.getException());
    assertEquals(TaskStatus.DONE, results.getTaskStatus());
    InOrder inOrder = inOrder(mockRdsClient);
    inOrder.verify(mockRdsClient).describeInstance(LIVE_PHYSICAL_NAME);
    inOrder.verify(mockRdsClient).createSnapshot(anyString(), eq(LIVE_PHYSICAL_NAME));
    inOrder.verify(mockRdsClient).copyParameterGroup(anyString(), eq(UGLY_STAGE_PARAM_GROUP_NAME));
    inOrder.verify(mockRdsClient).restoreInstanceFromSnapshot(eq(STAGE_PHYSICAL_NAME), anyString(), eq(SUBNET_GROUP));
    inOrder.verify(mockRdsClient).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), UGLY_STAGE_PARAM_GROUP_NAME);
    //Could also verify describeSnapshot
  }

  /**
   * Noop case, should call describeInstance but nothing else on the RDS api.
   */
  @Test
  public void testProcess_Noop() throws Throwable
  {
    ProcessResults results = testProcess(RdsInstanceStatus.AVAILABLE, RdsInstanceStatus.AVAILABLE,
        RdsParameterApplyStatus.PENDING_REBOOT, true);

    assertNoException(results.getException());
    assertEquals(TaskStatus.NOOP, results.getTaskStatus());
    verify(mockRdsClient).describeInstance(LIVE_PHYSICAL_NAME);
    verifyNoMoreInteractions(mockRdsClient);
  }
}