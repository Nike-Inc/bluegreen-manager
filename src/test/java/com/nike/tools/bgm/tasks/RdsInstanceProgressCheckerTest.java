package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBInstance;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsInstanceStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RdsInstanceProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context) ";
  private static final int WAIT_NUM = 1;
  private static final String INSTANCE_ID = "rds-instance-hello";
  private static final String ANOTHER_INSTANCE_ID = "rds-instance-goodbye";
  private static final String STATUS_UNKNOWN = "unknown";

  @Mock
  private RdsClient mockRdsClient;

  private RdsInstanceProgressChecker makeProgressChecker(DBInstance initialInstance,
                                                         RdsInstanceStatus expectedInitialState)
  {
    return new RdsInstanceProgressChecker(INSTANCE_ID, LOG_CONTEXT, mockRdsClient, initialInstance, expectedInitialState);
  }

  /**
   * Returns a progress checker whose initialInstance has an actual state equal to the expected initial state.
   */
  private RdsInstanceProgressChecker makeInitiallyOkProgressChecker(RdsInstanceStatus initialState)
  {
    return makeProgressChecker(fakeInstance(INSTANCE_ID, initialState), initialState);
  }

  /**
   * Test helper - makes a DBInstance
   */
  private DBInstance fakeInstance(String instanceId, RdsInstanceStatus currentStatus)
  {
    DBInstance dbInstance = new DBInstance();
    dbInstance.setDBInstanceIdentifier(instanceId);
    dbInstance.setDBInstanceStatus(currentStatus == null ? STATUS_UNKNOWN : currentStatus.toString());
    return dbInstance;
  }

  private void testGetDescription(String expectedSubstring, RdsInstanceStatus initialState)
  {
    RdsInstanceProgressChecker progressChecker = makeProgressChecker(fakeInstance(INSTANCE_ID, RdsInstanceStatus.AVAILABLE), initialState);
    assertTrue(progressChecker.getDescription().contains(expectedSubstring));
  }

  @Test
  public void testGetDescriptionAll()
  {
    testGetDescription("Create", RdsInstanceStatus.CREATING);
    testGetDescription("Modify", RdsInstanceStatus.MODIFYING);
    testGetDescription("Delete", RdsInstanceStatus.DELETING);
    testGetDescription("Reboot", RdsInstanceStatus.REBOOTING);
  }

  /**
   * Initial instance with the given acceptable initial status = fine, not done.
   */
  private void testInitialCheck_Acceptable(RdsInstanceStatus acceptableInitialStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker(acceptableInitialStatus);
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
  }

  @Test
  public void testInitialCheckCreate_Acceptable()
  {
    testInitialCheck_Acceptable(RdsInstanceStatus.CREATING);
  }

  @Test
  public void testInitialCheckModify_Acceptable()
  {
    testInitialCheck_Acceptable(RdsInstanceStatus.MODIFYING);
  }

  @Test
  public void testInitialCheckDelete_Acceptable()
  {
    testInitialCheck_Acceptable(RdsInstanceStatus.DELETING);
  }

  @Test
  public void testInitialCheckReboot_Acceptable()
  {
    testInitialCheck_Acceptable(RdsInstanceStatus.REBOOTING);
  }

  /**
   * Ask for initial status, describe shows initially doing other status = done with error (null result).
   */
  private void testInitialCheck_BadStatus(RdsInstanceStatus expectedInitialStatus,
                                          RdsInstanceStatus actualInstanceStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeProgressChecker(fakeInstance(INSTANCE_ID, actualInstanceStatus), expectedInitialStatus);
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
  }

  @Test
  public void testInitialCheckAll_BadStatus()
  {
    testInitialCheck_BadStatus(RdsInstanceStatus.CREATING, RdsInstanceStatus.DELETING);
    testInitialCheck_BadStatus(RdsInstanceStatus.MODIFYING, RdsInstanceStatus.DELETING);
    testInitialCheck_BadStatus(RdsInstanceStatus.DELETING, RdsInstanceStatus.AVAILABLE);
    testInitialCheck_BadStatus(RdsInstanceStatus.REBOOTING, RdsInstanceStatus.CREATING);
  }

  /**
   * Initially wrong instance id = throw.
   */
  private void testInitialCheck_WrongId(RdsInstanceStatus acceptableInitialStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeProgressChecker(fakeInstance(ANOTHER_INSTANCE_ID, acceptableInitialStatus), acceptableInitialStatus);
    progressChecker.initialCheck();
  }

  @Test(expected = IllegalStateException.class)
  public void testInitialCheckCreate_WrongId()
  {
    testInitialCheck_WrongId(RdsInstanceStatus.CREATING);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitialCheckModify_WrongId()
  {
    testInitialCheck_WrongId(RdsInstanceStatus.MODIFYING);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitialCheckDelete_WrongId()
  {
    testInitialCheck_WrongId(RdsInstanceStatus.DELETING);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitialCheckReboot_WrongId()
  {
    testInitialCheck_WrongId(RdsInstanceStatus.REBOOTING);
  }

  /**
   * Ask for initial status, describe shows final state = done, with good result.
   */
  private void testInitialCheck_DoneAlready(RdsInstanceStatus expectedInitialState, RdsInstanceStatus finalState)
  {
    DBInstance initialInstance = fakeInstance(INSTANCE_ID, finalState);
    RdsInstanceProgressChecker progressChecker = makeProgressChecker(initialInstance, expectedInitialState);
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertEquals(initialInstance, progressChecker.getResult());
  }

  @Test
  public void testInitialCheckAll_DoneAlready()
  {
    testInitialCheck_DoneAlready(RdsInstanceStatus.CREATING, RdsInstanceStatus.AVAILABLE);
    testInitialCheck_DoneAlready(RdsInstanceStatus.MODIFYING, RdsInstanceStatus.AVAILABLE);
    testInitialCheck_DoneAlready(RdsInstanceStatus.DELETING, RdsInstanceStatus.DELETED);
    testInitialCheck_DoneAlready(RdsInstanceStatus.REBOOTING, RdsInstanceStatus.AVAILABLE);
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
  private void testFollowupCheck_Intermediate(RdsInstanceStatus acceptableInitialStatus,
                                              RdsInstanceStatus acceptableFollowupStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker(acceptableInitialStatus);
    whenDescribeInstance(fakeInstance(INSTANCE_ID, acceptableFollowupStatus));
    progressChecker.followupCheck(WAIT_NUM);
    assertFalse(progressChecker.isDone());
    verifyDescribeInstance();
  }

  @Test
  public void testFollowupCheckCreate_Intermediate()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.CREATING, RdsInstanceStatus.CREATING);
  }

  @Test
  public void testFollowupCheckModify_Intermediate()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.MODIFYING, RdsInstanceStatus.MODIFYING);
  }

  @Test
  public void testFollowupCheckDelete_Intermediate()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.DELETING, RdsInstanceStatus.DELETING);
  }

  @Test
  public void testFollowupCheckReboot_Intermediate()
  {
    testFollowupCheck_Intermediate(RdsInstanceStatus.REBOOTING, RdsInstanceStatus.REBOOTING);
  }

  /**
   * Followup with bad status = end with error (i.e. null result).
   */
  private void testFollowupCheck_BadStatus(RdsInstanceStatus acceptableInitialStatus,
                                           RdsInstanceStatus badFollowupStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker(acceptableInitialStatus);
    whenDescribeInstance(fakeInstance(INSTANCE_ID, badFollowupStatus));
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
    verifyDescribeInstance();
  }

  @Test
  public void testFollowupCheckCreate_BadStatus()
  {
    testFollowupCheck_BadStatus(RdsInstanceStatus.CREATING, RdsInstanceStatus.DELETING);
  }

  @Test
  public void testFollowupCheckModify_BadStatus()
  {
    testFollowupCheck_BadStatus(RdsInstanceStatus.MODIFYING, RdsInstanceStatus.DELETING);
  }

  @Test
  public void testFollowupCheckDelete_BadStatus()
  {
    testFollowupCheck_BadStatus(RdsInstanceStatus.DELETING, RdsInstanceStatus.AVAILABLE);
  }

  @Test
  public void testFollowupCheckReboot_BadStatus()
  {
    testFollowupCheck_BadStatus(RdsInstanceStatus.REBOOTING, RdsInstanceStatus.CREATING);
  }

  /**
   * Followup shows wrong snapshot id = throw.
   */
  private void testFollowupCheck_WrongId(RdsInstanceStatus acceptableInitialStatus,
                                         RdsInstanceStatus acceptableFollowupStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker(acceptableInitialStatus);
    whenDescribeInstance(fakeInstance(ANOTHER_INSTANCE_ID, acceptableFollowupStatus));
    progressChecker.followupCheck(WAIT_NUM);
  }

  @Test(expected = IllegalStateException.class)
  public void testFollowupCheckCreate_WrongId()
  {
    testFollowupCheck_WrongId(RdsInstanceStatus.CREATING, RdsInstanceStatus.CREATING);
  }

  @Test(expected = IllegalStateException.class)
  public void testFollowupCheckModify_WrongId()
  {
    testFollowupCheck_WrongId(RdsInstanceStatus.MODIFYING, RdsInstanceStatus.MODIFYING);
  }

  @Test(expected = IllegalStateException.class)
  public void testFollowupCheckDelete_WrongId()
  {
    testFollowupCheck_WrongId(RdsInstanceStatus.DELETING, RdsInstanceStatus.DELETING);
  }

  @Test(expected = IllegalStateException.class)
  public void testFollowupCheckReboot_WrongId()
  {
    testFollowupCheck_WrongId(RdsInstanceStatus.REBOOTING, RdsInstanceStatus.REBOOTING);
  }

  /**
   * Followup shows final status = done.
   */
  private void testFollowupCheck_Final(RdsInstanceStatus acceptableInitialStatus, RdsInstanceStatus finalStatus)
  {
    RdsInstanceProgressChecker progressChecker = makeInitiallyOkProgressChecker(acceptableInitialStatus);
    DBInstance followupInstance = fakeInstance(INSTANCE_ID, finalStatus);
    whenDescribeInstance(followupInstance);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(followupInstance, progressChecker.getResult());
    verifyDescribeInstance();
  }

  @Test
  public void testFollowupCheckCreate_Final()
  {
    testFollowupCheck_Final(RdsInstanceStatus.CREATING, RdsInstanceStatus.AVAILABLE);
  }

  @Test
  public void testFollowupCheckModify_Final()
  {
    testFollowupCheck_Final(RdsInstanceStatus.MODIFYING, RdsInstanceStatus.AVAILABLE);
  }

  @Test
  public void testFollowupCheckDelete_Final()
  {
    testFollowupCheck_Final(RdsInstanceStatus.DELETING, RdsInstanceStatus.DELETED);
  }

  @Test
  public void testFollowupCheckReboot_Final()
  {
    testFollowupCheck_Final(RdsInstanceStatus.REBOOTING, RdsInstanceStatus.AVAILABLE);
  }
}