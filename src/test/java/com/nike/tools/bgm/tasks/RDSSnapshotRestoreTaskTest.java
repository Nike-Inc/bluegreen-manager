package com.nike.tools.bgm.tasks;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DBSubnetGroup;
import com.amazonaws.services.rds.model.Endpoint;
import com.nike.tools.bgm.client.aws.InstanceStatus;
import com.nike.tools.bgm.client.aws.RDSAnalyzer;
import com.nike.tools.bgm.client.aws.RDSCopier;
import com.nike.tools.bgm.client.aws.RDSCopierFactory;
import com.nike.tools.bgm.client.aws.SnapshotStatus;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.DatabaseTestHelper;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;

import static com.nike.tools.bgm.model.domain.DatabaseTestHelper.LIVE_ENV_NAME;
import static com.nike.tools.bgm.model.domain.DatabaseTestHelper.LIVE_LOGICAL_NAME;
import static com.nike.tools.bgm.model.domain.DatabaseTestHelper.LIVE_PHYSICAL_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * TODO - The high level restoreStage tests are too complicated...would help to modularize RDSSnapshotRestoreTask better, then tests would become simpler
 */
@RunWith(MockitoJUnitRunner.class)
public class RDSSnapshotRestoreTaskTest
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
  private final Environment FAKE_STAGE_ENV = new Environment() //non-static: needs to be reinitialized on every test
  {{
      setEnvName(STAGE_ENV_NAME);
    }};
  private static final Map<String, String> DB_MAP = new HashMap<String, String>()
  {{
      put(LIVE_LOGICAL_NAME, STAGE_PHYSICAL_NAME);
    }};

  @InjectMocks
  private RDSSnapshotRestoreTask rdsSnapshotRestoreTask;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Mock
  private RDSCopierFactory mockRdsCopierFactory;

  @Mock
  private RDSAnalyzer mockRdsAnalyzer;

  @Mock
  private RDSCopier mockRdsCopier;

  /**
   * Initializes the object-under-test for the "normal" case where live/stage envs meet preconditions.
   */
  private void normalSetup()
  {
    when(mockEnvironmentTx.findNamedEnv(LIVE_ENV_NAME)).thenReturn(FAKE_PHYSICAL_DATABASE.getLogicalDatabase().getEnvironment());
    when(mockEnvironmentTx.findNamedEnv(STAGE_ENV_NAME)).thenReturn(null);
    when(mockRdsCopierFactory.create()).thenReturn(mockRdsCopier);
    rdsSnapshotRestoreTask.init(1, LIVE_ENV_NAME, STAGE_ENV_NAME, DB_MAP);
  }

  //TODO - test init() WITHOUT normalSetup() ...i.e. error cases

  /**
   * Live context after init should show env/logical/physical.
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
   * Stage context after init should show env/logical/physical using stageContextFromArgs.
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
   * Tests the case where the snapshot is created with an unexpected id.
   */
  @Test(expected = IllegalStateException.class)
  public void testSnapshotLive_BadId()
  {
    normalSetup();
    DBSnapshot dbSnapshot = new DBSnapshot();
    dbSnapshot.setDBSnapshotIdentifier("The Wrong ID");
    when(mockRdsCopier.createSnapshot(anyString(), anyString())).thenReturn(dbSnapshot);

    rdsSnapshotRestoreTask.snapshotLive(false/*noop*/);
  }

  /**
   * Tests the case where the snapshot is created with an unexpected status.
   */
  @Test
  public void testSnapshotLive_Pass()
  {
    normalSetup();
    DBSnapshot dbSnapshot = new DBSnapshot();
    dbSnapshot.setDBSnapshotIdentifier(rdsSnapshotRestoreTask.makeSnapshotId());
    dbSnapshot.setStatus(SnapshotStatus.AVAILABLE.toString());
    when(mockRdsCopier.createSnapshot(anyString(), anyString())).thenReturn(dbSnapshot);

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
   */
  private static class RestoreStageFakeData
  {
    private static final String UNKNOWN_STATUS = "unknown";
    private DBSnapshot dbSnapshot = new DBSnapshot();
    private DBParameterGroup stageParamGroup = new DBParameterGroup();
    private DBInstance liveInstance = makeInstance(LIVE_PHYSICAL_NAME, InstanceStatus.AVAILABLE);
    private DBInstance stageRestoreInstance;
    private DBInstance stageModifyInstance;
    private Collection<String> securityGroups = Arrays.asList("security-group-1");

    public RestoreStageFakeData(InstanceStatus stageRestoreStatus,
                                InstanceStatus stageModifyStatus,
                                String stageParamGroupName,
                                String snapshotId)
    {
      dbSnapshot.setDBSnapshotIdentifier(snapshotId);
      dbSnapshot.setStatus(SnapshotStatus.AVAILABLE.toString());
      stageParamGroup.setDBParameterGroupName(stageParamGroupName);
      stageRestoreInstance = makeInstance(STAGE_PHYSICAL_NAME, stageRestoreStatus);
      stageModifyInstance = makeInstance(STAGE_PHYSICAL_NAME, stageModifyStatus);
    }

    private DBInstance makeInstance(String name, InstanceStatus status)
    {
      DBInstance dbInstance = new DBInstance();
      dbInstance.setDBInstanceIdentifier(name);
      dbInstance.setDBInstanceStatus(status == null ? UNKNOWN_STATUS : status.toString());
      Endpoint endpoint = new Endpoint();
      endpoint.setAddress(STAGE_ENDPOINT_ADDRESS);
      dbInstance.setEndpoint(endpoint);
      DBSubnetGroup dbSubnetGroup = new DBSubnetGroup();
      dbSubnetGroup.setDBSubnetGroupName(SUBNET_GROUP);
      dbInstance.setDBSubnetGroup(dbSubnetGroup);
      return dbInstance;
    }

    public DBSnapshot getDbSnapshot()
    {
      return dbSnapshot;
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
  private RestoreStageFakeData restoreSetup(InstanceStatus stageRestoreStatus,
                                            InstanceStatus stageModifyStatus,
                                            String stageParamGroupName,
                                            String snapshotId)
  {
    RestoreStageFakeData data = new RestoreStageFakeData(stageRestoreStatus, stageModifyStatus, stageParamGroupName, snapshotId);
    when(mockRdsCopier.restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, snapshotId, SUBNET_GROUP))
        .thenReturn(data.getStageRestoreInstance());
    when(mockRdsAnalyzer.extractVpcSecurityGroupIds(data.getLiveInstance())).thenReturn(data.getSecurityGroups());
    when(mockRdsCopier.modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), stageParamGroupName))
        .thenReturn(data.getStageModifyInstance());
    return data;
  }

  /**
   * Test setup and execution for restoreStage.
   *
   * @param stageRestoreStatus Instance status of stage physicaldb after restore-from-snapshot operation.
   * @param stageModifyStatus  Instance status of stage physicaldb after modify operation.
   */
  private RestoreStageResults testRestoreStage(InstanceStatus stageRestoreStatus,
                                               InstanceStatus stageModifyStatus,
                                               String stageParamGroupName)
  {
    normalSetup();
    RestoreStageFakeData data = restoreSetup(stageRestoreStatus, stageModifyStatus, stageParamGroupName, FAKE_SNAPSHOT_ID);
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
    RestoreStageResults results = testRestoreStage(InstanceStatus.AVAILABLE, InstanceStatus.AVAILABLE, SIMPLE_STAGE_PARAM_GROUP_NAME);
    RestoreStageFakeData data = results.getData();

    assertEquals(results.getResultInstance(), data.getStageModifyInstance());
    assertNull(results.getException());
    verify(mockRdsCopier).restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, FAKE_SNAPSHOT_ID, SUBNET_GROUP);
    verify(mockRdsCopier).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), SIMPLE_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Fail case: stage instance is restored into unknown status, should throw and not modify.
   */
  @Test
  public void testRestoreStage_FailRestore()
  {
    RestoreStageResults results = testRestoreStage(null, InstanceStatus.AVAILABLE, SIMPLE_STAGE_PARAM_GROUP_NAME);
    RestoreStageFakeData data = results.getData();

    assertNull(results.getResultInstance());
    assertEquals(RuntimeException.class, results.getException().getClass());
    verify(mockRdsCopier).restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, FAKE_SNAPSHOT_ID, SUBNET_GROUP);
    verify(mockRdsCopier, times(0)).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), SIMPLE_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Fail case: stage instance is restored ok but after modify is in unknown status, should throw.
   */
  @Test
  public void testRestoreStage_FailModify()
  {
    RestoreStageResults results = testRestoreStage(InstanceStatus.AVAILABLE, null, SIMPLE_STAGE_PARAM_GROUP_NAME);
    RestoreStageFakeData data = results.getData();

    assertNull(results.getResultInstance());
    assertEquals(RuntimeException.class, results.getException().getClass());
    verify(mockRdsCopier).restoreInstanceFromSnapshot(STAGE_PHYSICAL_NAME, FAKE_SNAPSHOT_ID, SUBNET_GROUP);
    verify(mockRdsCopier).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), SIMPLE_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Tests that we can init transient database entities for stage logical and physical.
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
    assertEquals(FAKE_STAGE_ENV, stageLogicalDatabase.getEnvironment());
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
   * @param stageRestoreStatus Instance status of stage physicaldb after restore-from-snapshot operation.
   * @param stageModifyStatus  Instance status of stage physicaldb after modify operation.
   */
  private ProcessResults testProcess(InstanceStatus stageRestoreStatus,
                                     InstanceStatus stageModifyStatus,
                                     boolean noop)
  {
    normalSetup();
    String snapshotId = rdsSnapshotRestoreTask.makeSnapshotId();
    RestoreStageFakeData data = restoreSetup(stageRestoreStatus, stageModifyStatus, UGLY_STAGE_PARAM_GROUP_NAME, snapshotId);
    when(mockRdsCopier.describeInstance(LIVE_PHYSICAL_NAME)).thenReturn(data.getLiveInstance());
    when(mockRdsCopier.createSnapshot(snapshotId, LIVE_PHYSICAL_NAME)).thenReturn(data.getDbSnapshot());
    when(mockRdsAnalyzer.findSelfNamedOrDefaultParamGroupName(data.getLiveInstance())).thenReturn(LIVE_PARAM_GROUP_NAME);
    when(mockRdsCopier.copyParameterGroup(LIVE_PARAM_GROUP_NAME, UGLY_STAGE_PARAM_GROUP_NAME)).thenReturn(data.getStageParamGroup());
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
    ProcessResults results = testProcess(InstanceStatus.AVAILABLE, InstanceStatus.AVAILABLE, false);
    RestoreStageFakeData data = results.getData();

    assertNoException(results.getException());
    assertEquals(TaskStatus.DONE, results.getTaskStatus());
    InOrder inOrder = inOrder(mockRdsCopier);
    inOrder.verify(mockRdsCopier).describeInstance(LIVE_PHYSICAL_NAME);
    inOrder.verify(mockRdsCopier).createSnapshot(anyString(), eq(LIVE_PHYSICAL_NAME));
    inOrder.verify(mockRdsCopier).copyParameterGroup(anyString(), eq(UGLY_STAGE_PARAM_GROUP_NAME));
    inOrder.verify(mockRdsCopier).restoreInstanceFromSnapshot(eq(STAGE_PHYSICAL_NAME), anyString(), eq(SUBNET_GROUP));
    inOrder.verify(mockRdsCopier).modifyInstanceWithSecgrpParamgrp(STAGE_PHYSICAL_NAME, data.getSecurityGroups(), UGLY_STAGE_PARAM_GROUP_NAME);
  }

  /**
   * Noop case, should call describeInstance but nothing else on the RDS api.
   */
  @Test
  public void testProcess_Noop() throws Throwable
  {
    ProcessResults results = testProcess(InstanceStatus.AVAILABLE, InstanceStatus.AVAILABLE, true);

    assertNoException(results.getException());
    assertEquals(TaskStatus.NOOP, results.getTaskStatus());
    verify(mockRdsCopier).describeInstance(LIVE_PHYSICAL_NAME);
    verifyNoMoreInteractions(mockRdsCopier);
  }
}