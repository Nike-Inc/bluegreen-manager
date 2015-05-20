package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.nike.tools.bgm.client.aws.RdsClient;

/**
 * Common activities in checking progress of an RDS snapshot.
 */
public abstract class RdsSnapshotProgressChecker
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsSnapshotProgressChecker.class);

  protected String snapshotId;
  protected String logContext;
  protected RdsClient rdsClient;
  protected DBSnapshot initialSnapshot;
  protected boolean done;

  public RdsSnapshotProgressChecker(String snapshotId,
                                    String logContext,
                                    RdsClient rdsClient, DBSnapshot initialSnapshot)
  {
    this.snapshotId = snapshotId;
    this.logContext = logContext;
    this.rdsClient = rdsClient;
    this.initialSnapshot = initialSnapshot;
  }

  public abstract String getDescription();

  /**
   * Checks initial response snapshot.
   */
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
   * Checks if the snapshot is in an acceptable intermediate status, maybe flags done if the end state can be detected.
   */
  protected abstract void checkSnapshotStatus(DBSnapshot dbSnapshot);

  public boolean isDone()
  {
    return done;
  }

  /**
   * Returns something when done, or null when not done or timeout.
   */
  public abstract Object getResult();
}
