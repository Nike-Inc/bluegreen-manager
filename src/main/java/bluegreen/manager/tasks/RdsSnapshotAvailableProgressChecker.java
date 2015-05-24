package bluegreen.manager.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBSnapshot;

import bluegreen.manager.client.aws.RdsClient;
import bluegreen.manager.client.aws.RdsSnapshotStatus;
import bluegreen.manager.utils.ProgressChecker;

/**
 * Knows how to check progress of an RDS snapshot going from 'creating' to 'available'.
 */
public class RdsSnapshotAvailableProgressChecker extends RdsSnapshotProgressChecker implements ProgressChecker<DBSnapshot>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsSnapshotAvailableProgressChecker.class);

  private DBSnapshot result;

  public RdsSnapshotAvailableProgressChecker(String snapshotId,
                                             String logContext,
                                             RdsClient rdsClient, DBSnapshot initialSnapshot)
  {
    super(snapshotId, logContext, rdsClient, initialSnapshot);
  }

  @Override
  public String getDescription()
  {
    return "RDS Create Snapshot '" + snapshotId + "'";
  }

  /**
   * Checks initial response snapshot.
   */
  @Override
  public void initialCheck()
  {
    super.initialCheck();
  }

  /**
   * Communicates with RDS for updated snapshot progress and checks the status.
   * Concludes if error or if naturally done.
   */
  @Override
  public void followupCheck(int waitNum)
  {
    super.followupCheck(waitNum);
  }

  /**
   * Checks if the snapshot is in an acceptable intermediate status, and flags done if status=available.
   */
  @Override
  protected void checkSnapshotStatus(DBSnapshot dbSnapshot)
  {
    final String status = dbSnapshot.getStatus();
    final String snapshotId = dbSnapshot.getDBSnapshotIdentifier();
    if (RdsSnapshotStatus.AVAILABLE.equalsString(status))
    {
      LOGGER.info(getDescription() + " is done");
      done = true;
      result = dbSnapshot;
    }
    else if (RdsSnapshotStatus.CREATING.equalsString(status))
    {
      //Keep waiting.
    }
    else
    {
      LOGGER.error(logContext + "Snapshot '" + snapshotId + "': Unexpected response status '" + status + "'");
      done = true;
    }
  }

  /**
   * Set to the final DBSnapshot object when snapshot is done.
   * Null if still creating or timeout.
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
