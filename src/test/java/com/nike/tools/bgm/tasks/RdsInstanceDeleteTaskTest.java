package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.nike.tools.bgm.client.aws.RdsAnalyzer;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsClientFactory;
import com.nike.tools.bgm.client.aws.RdsInstanceStatus;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.model.tx.OneEnvLoader;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.WaiterParameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the deletion of an rds instance, its parameter group, a db snapshot.
 */
@RunWith(MockitoJUnitRunner.class)
public class RdsInstanceDeleteTaskTest
{
  private static final String PARAM_GROUP_NAME = "some-param-group";

  @InjectMocks
  private RdsInstanceDeleteTask rdsInstanceDeleteTask;

  @Spy
  protected WaiterParameters fakeWaiterParameters = new WaiterParameters(10L, 10L, 2, 3);

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Mock
  private RdsClientFactory mockRdsClientFactory;

  @Mock
  private RdsAnalyzer mockRdsAnalyzer;

  @Mock
  private RdsClient mockRdsClient;

  @Mock
  private ThreadSleeper mockThreadSleeper;

  /*
  Note: the process(true) tests below will modify the deleteEnv.
   */
  private final Environment fakeLiveEnv = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private final Environment fakeDeleteEnv = EnvironmentTestHelper.makeFakeFullEnvironment(1); //isLive=false
  private final String instanceId = fakeDeleteEnv.getLogicalDatabases().get(0).getPhysicalDatabase().getInstanceName();

  private void setUp(Environment deleteEnv)
  {
    when(mockEnvLoaderFactory.createOne(deleteEnv.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(deleteEnv);
    when(mockOneEnvLoader.getLogicalDatabase()).thenReturn(deleteEnv.getLogicalDatabases().get(0));
    when(mockOneEnvLoader.getPhysicalDatabase()).thenReturn(deleteEnv.getLogicalDatabases().get(0).getPhysicalDatabase());
    rdsInstanceDeleteTask.assign(1, deleteEnv.getEnvName());
    rdsInstanceDeleteTask.loadDataModel();
  }

  private void resetMocks()
  {
    reset(mockEnvLoaderFactory, mockOneEnvLoader);
  }

  @Before
  public void setUp()
  {
    setUp(fakeDeleteEnv);
  }

  @Test
  public void testContext()
  {
    String context = rdsInstanceDeleteTask.context();
    assertTrue(context.contains(fakeDeleteEnv.getEnvName()));
  }

  /**
   * Normal setup should specify a deleteEnv with isLive==false.
   */
  @Test
  public void testCheckDeleteDatabaseIsNotLive_Pass()
  {
    rdsInstanceDeleteTask.checkDeleteDatabaseIsNotLive();
  }

  /**
   * VERY IMPORTANT - "deleteEnv" with isLive==true should be flagged immediately.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCheckDeleteDatabaseIsNotLive_Fail()
  {
    resetMocks();
    setUp(fakeLiveEnv);
    rdsInstanceDeleteTask.checkDeleteDatabaseIsNotLive();
  }

  /**
   * Simulate deleting the rds instance and waiting for confirmation.
   */
  private void testDeleteInstance(RdsInstanceStatus thirdStatus)
  {
    when(mockRdsClient.deleteInstance(instanceId)).thenReturn(fakeInstance(RdsInstanceStatus.DELETING/*status #0*/));
    if (thirdStatus != null)
    {
      when(mockRdsClient.describeInstance(instanceId))
          .thenReturn(fakeInstance(RdsInstanceStatus.DELETING)) //Followup waitNum#1
          .thenReturn(fakeInstance(RdsInstanceStatus.DELETING)) //Followup waitNum#2
          .thenReturn(fakeInstance(thirdStatus));
    }
    else
    {
      when(mockRdsClient.describeInstance(instanceId))
          .thenReturn(fakeInstance(RdsInstanceStatus.DELETING)) //Followup waitNum#1
          .thenReturn(fakeInstance(RdsInstanceStatus.DELETING)) //Followup waitNum#2
          .thenThrow(new DBInstanceNotFoundException("not found")); //Checker should catch it
    }

    rdsInstanceDeleteTask.deleteInstance(false);

    verify(mockRdsClient, times(3)).describeInstance(instanceId);
  }

  /**
   * Test helper - makes a DBInstance
   */
  private DBInstance fakeInstance(RdsInstanceStatus currentStatus)
  {
    DBInstance dbInstance = new DBInstance();
    dbInstance.setDBInstanceIdentifier(instanceId);
    dbInstance.setDBInstanceStatus(currentStatus.toString());
    return dbInstance;
  }

  @Test
  public void testDeleteInstance_TeardownDeleted()
  {
    testDeleteInstance(RdsInstanceStatus.DELETED); //Done
  }

  @Test
  public void testDeleteInstance_TeardownNotFound()
  {
    testDeleteInstance(null); //Not Found = Done
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteInstance_TeardownFailed()
  {
    testDeleteInstance(RdsInstanceStatus.DELETING); //Not done -> Timeout
  }

  private void testDeleteParameterGroup(String paramGroupName)
  {
    DBInstance dbInstance = fakeInstance(RdsInstanceStatus.DELETING);
    when(mockRdsAnalyzer.findSelfNamedParamGroupName(dbInstance)).thenReturn(paramGroupName);

    rdsInstanceDeleteTask.deleteParameterGroup(dbInstance, false);
  }

  /**
   * If the db instance used the default parameter group, then the task won't use the rdsClient to delete it.
   */
  @Test
  public void testDeleteParameterGroup_DefaultGroup()
  {
    testDeleteParameterGroup(null);
    verifyZeroInteractions(mockRdsClient);
  }

  /**
   * If the db instance had its own parameter group, then expect the rdsClient will delete it.
   */
  @Test
  public void testDeleteParameterGroup_YesDelete()
  {
    testDeleteParameterGroup(PARAM_GROUP_NAME);
    verify(mockRdsClient).deleteParameterGroup(PARAM_GROUP_NAME);
  }

  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, rdsInstanceDeleteTask.process(true));
  }

  /**
   * Runs process all the way through.  Should be same in teardown and rollback test scenarios, except for the
   * value of snapshotId (which is only specified as 'anyString' here).
   * <p/>
   * (In fact for a "real" deploy situation the snapshotId would be same whether teardown or rollback.  The only reason
   * it is different here is because we're overloading fakeDeleteEnv as an old live env for teardown tests and as the
   * stage env for rollback tests.)
   * <p/>
   * Calls loadDataModel twice, should be ok.
   */
  @Test
  public void testProcess()
  {
    when(mockRdsClientFactory.create()).thenReturn(mockRdsClient);
    when(mockRdsClient.deleteInstance(instanceId)).thenReturn(fakeInstance(RdsInstanceStatus.DELETED));
    when(mockRdsAnalyzer.findSelfNamedParamGroupName(any(DBInstance.class))).thenReturn(PARAM_GROUP_NAME);
    assertEquals(TaskStatus.DONE, rdsInstanceDeleteTask.process(false));
    verify(mockRdsClient).deleteInstance(instanceId);
    verify(mockRdsClient).deleteParameterGroup(PARAM_GROUP_NAME);
    verify(mockEnvironmentTx).updateEnvironment(fakeDeleteEnv);
  }

}