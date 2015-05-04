package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.SnapshotStatus;
import com.nike.tools.bgm.utils.ProgressChecker;

/**
 * Knows how to check progress of an RDS snapshot going from 'creating' to 'available'.
 */
public class SnapshotProgressChecker implements ProgressChecker<DBSnapshot>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotProgressChecker.class);

  private String snapshotId;
  private String logContext;
  private RdsClient rdsClient;
  private DBSnapshot initialSnapshot;
  private boolean done;
  private DBSnapshot result;

  public SnapshotProgressChecker(String snapshotId,
                                 String logContext,
                                 RdsClient rdsClient, DBSnapshot initialSnapshot)
  {
    this.snapshotId = snapshotId;
    this.logContext = logContext;
    this.rdsClient = rdsClient;
    this.initialSnapshot = initialSnapshot;
  }

  @Override
  public String getDescription()
  {
    return "Create Snapshot '" + snapshotId + "'";
  }

  /**
   * Checks initial response snapshot.
   */
  @Override
  public void initialCheck()
  {
    LOGGER.debug("Initial RDS snapshot status: " + initialSnapshot.getStatus());
    checkSnapshotId(initialSnapshot);
    checkSnapshotStatus(initialSnapshot);
  }

  /**
   * Communicates with RDS for updated snapshot progress and checks the status.
   * Concludes if error or if naturally done.
   */
  @Override
  public void followupCheck(int waitNum)
  {
    DBSnapshot dbSnapshot = rdsClient.describeSnapshot(snapshotId);
    checkSnapshotId(dbSnapshot);
    LOGGER.debug(logContext + "RDS snapshot status after wait#" + waitNum + ": " + dbSnapshot.getStatus());
    checkSnapshotStatus(dbSnapshot);
  }

  /**
   * Asserts that the snapshot has the expected id.
   */
  private void checkSnapshotId(DBSnapshot dbSnapshot)
  {
    final String responseSnapshotId = dbSnapshot.getDBSnapshotIdentifier();
    if (!StringUtils.equals(snapshotId, responseSnapshotId))
    {
      throw new IllegalStateException(logContext + "We requested snapshot id '" + snapshotId
          + "' but RDS replied with identifier '" + responseSnapshotId + "'");
    }
  }

  /**
   * Checks if the snapshot is in an acceptable intermediate status, and flags done if status=available.
   */
  private void checkSnapshotStatus(DBSnapshot dbSnapshot)
  {
    final String status = dbSnapshot.getStatus();
    final String snapshotId = dbSnapshot.getDBSnapshotIdentifier();
    if (SnapshotStatus.AVAILABLE.equalsString(status))
    {
      LOGGER.info("RDS Snapshot '" + snapshotId + "' is done");
      done = true;
      result = dbSnapshot;
    }
    else if (SnapshotStatus.CREATING.equalsString(status))
    {
      //Keep waiting.
    }
    else
    {
      LOGGER.error(logContext + "Snapshot '" + snapshotId + "': Unexpected response status '" + status + "'");
      done = true;
    }
  }

  @Override
  public boolean isDone()
  {
    return done;
  }

  /**
   * True if the snapshot has become available prior to timeout.
   * False if error.  Null if still creating or timeout.
   */
  @Override
  public DBSnapshot getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns null.
   */
  @Override
  public DBSnapshot timeout()
  {
    LOGGER.error("Snapshot failed to become available prior to timeout");
    return null;
  }
}
