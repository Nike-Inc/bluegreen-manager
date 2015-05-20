package com.nike.tools.bgm.tasks;

import org.mockito.Mock;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsSnapshotStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RdsSnapshotProgressCheckerTestBase
{
  protected static final String LOG_CONTEXT = "(Log Context) ";
  protected static final int WAIT_NUM = 1;
  protected static final String SNAPSHOT_ID = "the-snapshot-123";
  protected static final String ANOTHER_SNAPSHOT_ID = "another-snapshot-456";
  protected static final String INSTANCE_ID = "rds-instance-hello";
  protected static final String STATUS_UNKNOWN = "unknown";

  @Mock
  protected RdsClient mockRdsClient;

  /**
   * Test helper - makes a DBSnapshot
   */
  protected DBSnapshot fakeSnapshot(String snapshotId, RdsSnapshotStatus status)
  {
    DBSnapshot dbSnapshot = new DBSnapshot();
    dbSnapshot.setDBSnapshotIdentifier(snapshotId);
    dbSnapshot.setDBInstanceIdentifier(INSTANCE_ID);
    dbSnapshot.setStatus(status == null ? STATUS_UNKNOWN : status.toString());
    return dbSnapshot;
  }

  protected void testGetDescription(RdsSnapshotProgressChecker progressChecker)
  {
    assertTrue(progressChecker.getDescription().contains(SNAPSHOT_ID));
  }

  /**
   * Initial state is ok, just not done.
   */
  protected void testInitialOkNotDone(RdsSnapshotProgressChecker progressChecker)
  {
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
  }

  /**
   * Initial state is unexpected.  Recovers and ends with error.
   */
  protected void testInitialError(RdsSnapshotProgressChecker progressChecker)
  {
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
  }

  /**
   * Prepare the mock return value of describeSnapshot.
   */
  protected void whenDescribeSnapshot(DBSnapshot dbSnapshot)
  {
    when(mockRdsClient.describeSnapshot(SNAPSHOT_ID)).thenReturn(dbSnapshot);
  }

  /**
   * Verifies the mock was called.
   */
  protected void verifyDescribeSnapshot()
  {
    verify(mockRdsClient).describeSnapshot(SNAPSHOT_ID);
  }

  /**
   * Followup returns ok status, not done.
   */
  protected void testFollowupCheck_OkNotDone(RdsSnapshotProgressChecker progressChecker,
                                             RdsSnapshotStatus followupStatus)
  {
    whenDescribeSnapshot(fakeSnapshot(SNAPSHOT_ID, followupStatus));
    progressChecker.followupCheck(WAIT_NUM);
    assertFalse(progressChecker.isDone());
    verifyDescribeSnapshot();
  }

  /**
   * Followup shows unexpected state = end with error.
   */
  protected void testFollowupCheck_Error(RdsSnapshotProgressChecker progressChecker, RdsSnapshotStatus followupStatus)
  {
    whenDescribeSnapshot(fakeSnapshot(SNAPSHOT_ID, followupStatus));
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
    verifyDescribeSnapshot();
  }

  /**
   * Followup shows wrong snapshot id = throw.
   */
  protected void testFollowupCheck_WrongId(RdsSnapshotProgressChecker progressChecker, RdsSnapshotStatus followupStatus)
  {
    whenDescribeSnapshot(fakeSnapshot(ANOTHER_SNAPSHOT_ID, followupStatus));
    progressChecker.followupCheck(WAIT_NUM);
  }

  /**
   * Followup status = done.
   */
  protected void testFollowupCheck_Done(RdsSnapshotProgressChecker progressChecker, RdsSnapshotStatus followupStatus)
  {
    DBSnapshot followupSnapshot = fakeSnapshot(SNAPSHOT_ID, followupStatus);
    whenDescribeSnapshot(followupSnapshot);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(followupSnapshot, progressChecker.getResult());
    verifyDescribeSnapshot();
  }
}
