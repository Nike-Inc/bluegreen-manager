package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DBSnapshotNotFoundException;
import com.nike.tools.bgm.client.aws.RdsSnapshotStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RdsSnapshotDeletedProgressCheckerTest extends RdsSnapshotProgressCheckerTestBase
{
  private RdsSnapshotDeletedProgressChecker makeProgressChecker(DBSnapshot initialSnapshot)
  {
    return new RdsSnapshotDeletedProgressChecker(SNAPSHOT_ID, LOG_CONTEXT, mockRdsClient, initialSnapshot);
  }

  @Test
  public void testGetDescription()
  {
    testGetDescription(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING)));
  }

  /**
   * Initially deleting = not done.
   */
  @Test
  public void testInitialCheck_Deleting()
  {
    testInitialOkNotDone(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING)));
  }

  // Not testing "initially available" ...I'm not sure if Amazon thinks it is an ok thing.

  /**
   * Initially creating = end with error.
   */
  @Test
  public void testInitialCheck_Creating()
  {
    testInitialError(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING)));
  }

  /**
   * Initially wrong snapshot id = throw.
   */
  @Test(expected = IllegalStateException.class)
  public void testInitialCheck_WrongId()
  {
    RdsSnapshotDeletedProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(ANOTHER_SNAPSHOT_ID, RdsSnapshotStatus.DELETING));
    progressChecker.initialCheck();
  }

  /**
   * Followup shows deleting = not done.
   */
  @Test
  public void testFollowupCheck_Deleting()
  {
    testFollowupCheck_OkNotDone(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING)),
        RdsSnapshotStatus.DELETING);
  }

  /**
   * Followup shows creating = end with error.
   */
  @Test
  public void testFollowupCheck_Creating()
  {
    testFollowupCheck_Error(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING)),
        RdsSnapshotStatus.CREATING);
  }

  /**
   * Followup shows wrong snapshot id = throw.
   */
  @Test(expected = IllegalStateException.class)
  public void testFollowupCheck_WrongId()
  {
    testFollowupCheck_WrongId(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING)),
        RdsSnapshotStatus.DELETING);
  }

  @Test
  public void testFollowupCheck_Deleted()
  {
    RdsSnapshotDeletedProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING));
    when(mockRdsClient.describeSnapshot(SNAPSHOT_ID)).thenThrow(DBSnapshotNotFoundException.class);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(true, progressChecker.getResult());
    verifyDescribeSnapshot();
  }

}