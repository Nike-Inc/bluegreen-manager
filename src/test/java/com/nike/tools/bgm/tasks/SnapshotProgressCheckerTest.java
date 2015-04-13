package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RDSCopier;
import com.nike.tools.bgm.client.aws.SnapshotStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context) ";
  private static final int WAIT_NUM = 1;
  private static final String SNAPSHOT_ID = "the-snapshot-123";
  private static final String ANOTHER_SNAPSHOT_ID = "another-snapshot-456";
  private static final String INSTANCE_ID = "rds-instance-hello";
  private static final String STATUS_UNKNOWN = "unknown";

  @Mock
  private RDSCopier mockRdsCopier;

  private SnapshotProgressChecker makeProgressChecker(DBSnapshot initialSnapshot)
  {
    return new SnapshotProgressChecker(SNAPSHOT_ID, LOG_CONTEXT, mockRdsCopier, initialSnapshot);
  }

  /**
   * Test helper - makes a DBSnapshot
   */
  private DBSnapshot fakeSnapshot(String snapshotId, SnapshotStatus status)
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
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.AVAILABLE));
    assertTrue(progressChecker.getDescription().contains(SNAPSHOT_ID));
  }

  /**
   * Initially creating = not done.
   */
  @Test
  public void testInitialCheck_Creating()
  {
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.CREATING));
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
  }

  /**
   * Initially deleting = end with error.
   */
  @Test
  public void testInitialCheck_Deleting()
  {
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.DELETING));
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
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(ANOTHER_SNAPSHOT_ID, SnapshotStatus.CREATING));
    progressChecker.initialCheck();
  }

  /**
   * Initially available = done.
   */
  @Test
  public void testInitialCheck_Available()
  {
    DBSnapshot initialSnapshot = fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.AVAILABLE);
    SnapshotProgressChecker progressChecker = makeProgressChecker(initialSnapshot);
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertEquals(initialSnapshot, progressChecker.getResult());
  }

  /**
   * Prepare the mock return value of describeSnapshot.
   */
  private void whenDescribeSnapshot(DBSnapshot dbSnapshot)
  {
    when(mockRdsCopier.describeSnapshot(SNAPSHOT_ID)).thenReturn(dbSnapshot);
  }

  /**
   * Verifies the mock was called.
   */
  private void verifyDescribeSnapshot()
  {
    verify(mockRdsCopier).describeSnapshot(SNAPSHOT_ID);
  }

  /**
   * Followup shows creating = not done.
   */
  @Test
  public void testFollowupCheck_Creating()
  {
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.CREATING));
    whenDescribeSnapshot(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.CREATING));
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
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.CREATING));
    whenDescribeSnapshot(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.DELETING));
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
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.CREATING));
    whenDescribeSnapshot(fakeSnapshot(ANOTHER_SNAPSHOT_ID, SnapshotStatus.CREATING));
    progressChecker.followupCheck(WAIT_NUM);
  }

  /**
   * Followup shows available = done.
   */
  @Test
  public void testFollowupCheck_Available()
  {
    SnapshotProgressChecker progressChecker = makeProgressChecker(fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.CREATING));
    DBSnapshot followupSnapshot = fakeSnapshot(SNAPSHOT_ID, SnapshotStatus.AVAILABLE);
    whenDescribeSnapshot(followupSnapshot);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertEquals(followupSnapshot, progressChecker.getResult());
    verifyDescribeSnapshot();
  }
}