package bluegreen.manager.tasks;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroupStatus;

import bluegreen.manager.client.aws.RdsAnalyzer;
import bluegreen.manager.client.aws.RdsClient;
import bluegreen.manager.client.aws.RdsInstanceStatus;
import bluegreen.manager.client.aws.RdsParameterApplyStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RdsInstanceParamGroupProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context) ";
  private static final int WAIT_NUM = 1;
  private static final String INSTANCE_ID = "rds-instance-hello";
  private static final String PARAM_GROUP_NAME = "rds-param-group";
  private static final String STATUS_UNKNOWN = "unknown";

  @Mock
  private RdsClient mockRdsClient;

  @Spy
  private RdsAnalyzer mockRdsAnalyzer;

  private RdsInstanceParamGroupProgressChecker makeProgressChecker(DBInstance initialInstance,
                                                                   RdsInstanceStatus expectedInitialState)
  {
    return new RdsInstanceParamGroupProgressChecker(INSTANCE_ID, PARAM_GROUP_NAME, LOG_CONTEXT, mockRdsClient,
        mockRdsAnalyzer, initialInstance, expectedInitialState);
  }

  /**
   * Returns a progress checker whose initialInstance has an actual state equal to the expected initial state.
   */
  private RdsInstanceParamGroupProgressChecker makeInitiallyOkProgressChecker()
  {
    return makeProgressChecker(fakeKnownInstance(RdsInstanceStatus.MODIFYING, RdsParameterApplyStatus.APPLYING),
        RdsInstanceStatus.MODIFYING);
  }

  /**
   * Test helper - makes a DBInstance
   */
  private DBInstance fakeInstance(String instanceId, RdsInstanceStatus currentInstanceStatus,
                                  String paramGroupName, RdsParameterApplyStatus currentParameterApplyStatus)
  {
    DBInstance dbInstance = new DBInstance();
    dbInstance.setDBInstanceIdentifier(instanceId);
    dbInstance.setDBInstanceStatus(currentInstanceStatus == null ? STATUS_UNKNOWN : currentInstanceStatus.toString());
    if (paramGroupName != null)
    {
      List<DBParameterGroupStatus> list = new ArrayList<DBParameterGroupStatus>();
      DBParameterGroupStatus paramGroup = new DBParameterGroupStatus();
      paramGroup.setDBParameterGroupName(paramGroupName);
      paramGroup.setParameterApplyStatus(currentParameterApplyStatus == null ? STATUS_UNKNOWN : currentParameterApplyStatus.toString());
      list.add(paramGroup);
      dbInstance.setDBParameterGroups(list);
    }
    return dbInstance;
  }

  /**
   * Makes an instance that is "known": uses the expected instance id and paramgroup name.
   */
  private DBInstance fakeKnownInstance(RdsInstanceStatus currentInstanceStatus,
                                       RdsParameterApplyStatus currentParameterApplyStatus)
  {
    return fakeInstance(INSTANCE_ID, currentInstanceStatus, PARAM_GROUP_NAME, currentParameterApplyStatus);
  }

  private void testGetDescription(String expectedSubstring, RdsInstanceStatus initialState)
  {
    RdsInstanceProgressChecker progressChecker = makeProgressChecker(
        fakeKnownInstance(RdsInstanceStatus.AVAILABLE, RdsParameterApplyStatus.APPLYING), initialState);
    assertTrue(progressChecker.getDescription().contains(expectedSubstring));
  }

  @Test
  public void testGetDescription_Pass()
  {
    testGetDescription("Modify", RdsInstanceStatus.MODIFYING);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetDescription_WrongInitialState()
  {
    testGetDescription("Create", RdsInstanceStatus.CREATING);
  }

  /**
   * Initial instance with the given acceptable initial status = fine, not done.
   */
  @Test
  public void testInitialCheck_Acceptable()
  {
    RdsInstanceParamGroupProgressChecker progressChecker = makeInitiallyOkProgressChecker();
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
  }

  /**
   * Ask for initial status, describe shows final state = done, with good result.
   */
  private void testInitialCheck_DoneAlready(RdsInstanceStatus expectedInitialState,
                                            RdsInstanceStatus finalInstanceStatus,
                                            RdsParameterApplyStatus finalParameterApplyStatus)
  {
    DBInstance initialInstance = fakeKnownInstance(finalInstanceStatus, finalParameterApplyStatus);
    RdsInstanceProgressChecker progressChecker = makeProgressChecker(initialInstance, expectedInitialState);
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertEquals(initialInstance, progressChecker.getResult());
  }

  @Test
  public void testInitialCheck_DoneAlready()
  {
    testInitialCheck_DoneAlready(RdsInstanceStatus.MODIFYING,
        RdsInstanceStatus.AVAILABLE, RdsParameterApplyStatus.PENDING_REBOOT);
  }

  /**
   * Prepare the mock return value of describeInstance.
   */
  private void whenDescribeInstance(DBInstance dbInstance)
  {
    when(mockRdsClient.describeInstance(INSTANCE_ID)).thenReturn(dbInstance);
  }

  /**
   * Verifies the mock was called.
   */
  private void verifyDescribeInstance()
  {
    verify(mockRdsClient).describeInstance(INSTANCE_ID);
  }


  /**
   * Followup with the given acceptable continuing state = not done.
   */
  private void testFollowupCheck_Intermediate(RdsInstanceStatus acceptableFollowupInstanceStatus,
                                              RdsParameterApplyStatus acceptableFollowupParameterStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker();
    whenDescribeInstance(fakeKnownInstance(acceptableFollowupInstanceStatus, acceptableFollowupParameterStatus));
    progressChecker.followupCheck(WAIT_NUM);
    assertFalse(progressChecker.isDone());
    verifyDescribeInstance();
  }

  @Test
  public void testFollowupCheckIntermediate_ModifyingApplying()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.MODIFYING, RdsParameterApplyStatus.APPLYING);
  }

  @Test
  public void testFollowupCheckIntermediate_AvailableApplying()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.AVAILABLE, RdsParameterApplyStatus.APPLYING);
  }

  @Test
  public void testFollowupCheckIntermediate_ModifyingPendingReboot()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.MODIFYING, RdsParameterApplyStatus.PENDING_REBOOT);
  }

  /**
   * Followup with bad status = end with error (i.e. null result).
   * <p/>
   * One or both of the followup statuses should be bad.
   */
  private void testFollowupCheck_BadStatus(RdsInstanceStatus badFollowupInstanceStatus,
                                           RdsParameterApplyStatus badFollowupParameterStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker();
    whenDescribeInstance(fakeKnownInstance(badFollowupInstanceStatus, badFollowupParameterStatus));
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
    verifyDescribeInstance();
  }

  @Test
  public void testFollowupCheckBadStatus_Creating()
  {
    testFollowupCheck_BadStatus(RdsInstanceStatus.CREATING, RdsParameterApplyStatus.APPLYING);
  }

  @Test
  public void testFollowupCheckBadStatus_InSync()
  {
    testFollowupCheck_BadStatus(RdsInstanceStatus.MODIFYING, RdsParameterApplyStatus.IN_SYNC);
  }

  /**
   * Followup shows final status = done.
   */
  @Test
  public void testFollowupCheck_Final()
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker();
    DBInstance followupInstance = fakeKnownInstance(RdsInstanceStatus.AVAILABLE, RdsParameterApplyStatus.PENDING_REBOOT);
    whenDescribeInstance(followupInstance);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(followupInstance, progressChecker.getResult());
    verifyDescribeInstance();
  }

}