package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsSnapshotStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RdsSnapshotAvailableProgressCheckerTest extends RdsSnapshotProgressCheckerTestBase
{
  private RdsSnapshotAvailableProgressChecker makeProgressChecker(DBSnapshot initialSnapshot)
  {
    return new RdsSnapshotAvailableProgressChecker(SNAPSHOT_ID, LOG_CONTEXT, mockRdsClient, initialSnapshot);
  }

  @Test
  public void testGetDescription()
  {
    testGetDescription(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.AVAILABLE)));
  }

  /**
   * Initially creating = not done.
   */
  @Test
  public void testInitialCheck_Creating()
  {
    testInitialOkNotDone(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING)));
  }

  /**
   * Initially deleting = end with error.
   */
  @Test
  public void testInitialCheck_Deleting()
  {
    testInitialError(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING)));
  }

  /**
   * Initially wrong snapshot id = throw.
   */
  @Test(expected = IllegalStateException.class)
  public void testInitialCheck_WrongId()
  {
    RdsSnapshotAvailableProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(ANOTHER_SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    progressChecker.initialCheck();
  }

  /**
   * Initially available = done.
   */
  @Test
  public void testInitialCheck_Available()
  {
    DBSnapshot initialSnapshot = fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.AVAILABLE);
    RdsSnapshotAvailableProgressChecker progressChecker = makeProgressChecker(initialSnapshot);
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertEquals(initialSnapshot, progressChecker.getResult());
  }

  /**
   * Followup shows creating = not done.
   */
  @Test
  public void testFollowupCheck_Creating()
  {
    testFollowupCheck_OkNotDone(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING)),
        RdsSnapshotStatus.CREATING);
  }

  /**
   * Followup shows deleting = end with error.
   */
  @Test
  public void testFollowupCheck_Deleting()
  {
    testFollowupCheck_Error(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING)),
        RdsSnapshotStatus.DELETING);
  }

  /**
   * Followup shows wrong snapshot id = throw.
   */
  @Test(expected = IllegalStateException.class)
  public void testFollowupCheck_WrongId()
  {
    testFollowupCheck_WrongId(makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING)),
        RdsSnapshotStatus.CREATING);
  }

  /**
   * Followup shows available = done.
   */
  @Test
  public void testFollowupCheck_Available()
  {
    RdsSnapshotAvailableProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    DBSnapshot followupSnapshot = fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.AVAILABLE);
    whenDescribeSnapshot(followupSnapshot);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(followupSnapshot, progressChecker.getResult());
    verifyDescribeSnapshot();
  }
}