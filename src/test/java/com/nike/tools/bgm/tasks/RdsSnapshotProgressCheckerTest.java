package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsSnapshotStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RdsSnapshotProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context) ";
  private static final int WAIT_NUM = 1;
  private static final String SNAPSHOT_ID = "the-snapshot-123";
  private static final String ANOTHER_SNAPSHOT_ID = "another-snapshot-456";
  private static final String INSTANCE_ID = "rds-instance-hello";
  private static final String STATUS_UNKNOWN = "unknown";

  @Mock
  private RdsClient mockRdsClient;

  private RdsSnapshotProgressChecker makeProgressChecker(DBSnapshot initialSnapshot)
  {
    return new RdsSnapshotProgressChecker(SNAPSHOT_ID, LOG_CONTEXT, mockRdsClient, initialSnapshot);
  }

  /**
   * Test helper - makes a DBSnapshot
   */
  private DBSnapshot fakeSnapshot(String snapshotId, RdsSnapshotStatus status)
  {
    DBSnapshot dbSnapshot = new DBSnapshot();
    dbSnapshot.setDBSnapshotIdentifier(snapshotId);
    dbSnapshot.setDBInstanceIdentifier(INSTANCE_ID);
    dbSnapshot.setStatus(status == null ? STATUS_UNKNOWN : status.toString());
    return dbSnapshot;
  }

  @Test
  public void testGetDescription()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.AVAILABLE));
    assertTrue(progressChecker.getDescription().contains(SNAPSHOT_ID));
  }

  /**
   * Initially creating = not done.
   */
  @Test
  public void testInitialCheck_Creating()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
  }

  /**
   * Initially deleting = end with error.
   */
  @Test
  public void testInitialCheck_Deleting()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING));
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
  }

  /**
   * Initially wrong snapshot id = throw.
   */
  @Test(expected = IllegalStateException.class)
  public void testInitialCheck_WrongId()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(ANOTHER_SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    progressChecker.initialCheck();
  }

  /**
   * Initially available = done.
   */
  @Test
  public void testInitialCheck_Available()
  {
    DBSnapshot initialSnapshot = fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.AVAILABLE);
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(initialSnapshot);
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertEquals(initialSnapshot, progressChecker.getResult());
  }

  /**
   * Prepare the mock return value of describeSnapshot.
   */
  private void whenDescribeSnapshot(DBSnapshot dbSnapshot)
  {
    when(mockRdsClient.describeSnapshot(SNAPSHOT_ID)).thenReturn(dbSnapshot);
  }

  /**
   * Verifies the mock was called.
   */
  private void verifyDescribeSnapshot()
  {
    verify(mockRdsClient).describeSnapshot(SNAPSHOT_ID);
  }

  /**
   * Followup shows creating = not done.
   */
  @Test
  public void testFollowupCheck_Creating()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    whenDescribeSnapshot(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    progressChecker.followupCheck(WAIT_NUM);
    assertFalse(progressChecker.isDone());
    verifyDescribeSnapshot();
  }

  /**
   * Followup shows deleting = end with error.
   */
  @Test
  public void testFollowupCheck_Deleting()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    whenDescribeSnapshot(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.DELETING));
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertNull(progressChecker.getResult());
    verifyDescribeSnapshot();
  }

  /**
   * Followup shows wrong snapshot id = throw.
   */
  @Test(expected = IllegalStateException.class)
  public void testFollowupCheck_WrongId()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    whenDescribeSnapshot(fakeSnapshot(ANOTHER_SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    progressChecker.followupCheck(WAIT_NUM);
  }

  /**
   * Followup shows available = done.
   */
  @Test
  public void testFollowupCheck_Available()
  {
    RdsSnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.CREATING));
    DBSnapshot followupSnapshot = fakeSnapshot(SNAPSHOT_ID, RdsSnapshotStatus.AVAILABLE);
    whenDescribeSnapshot(followupSnapshot);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(followupSnapshot, progressChecker.getResult());
    verifyDescribeSnapshot();
  }
}