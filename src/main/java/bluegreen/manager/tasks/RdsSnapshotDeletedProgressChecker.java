package bluegreen.manager.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DBSnapshotNotFoundException;

import bluegreen.manager.client.aws.RdsClient;
import bluegreen.manager.client.aws.RdsSnapshotStatus;
import bluegreen.manager.utils.ProgressChecker;

/**
 * Checks progress of an RDS snapshot undergoing deletion.
 * <p/>
 * <a href="http://docs.aws.amazon.com/AmazonRDS/latest/CommandLineReference/CLIReference-cmd-DescribeDBSnapshots.html">
 * Amazon documentation</a> says the only snapshot status values are <tt>creating | available | deleting</tt>
 * ...but they lie, look at this result:
 * <pre>
 * DEBUG org.apache.http.impl.conn.Wire.wire(72) -  << "  <DeleteDBSnapshotResult>[\n]"
 * DEBUG org.apache.http.impl.conn.Wire.wire(72) -  << "    <DBSnapshot>[\n]"
 * DEBUG org.apache.http.impl.conn.Wire.wire(72) -  << "      <Status>deleted</Status>[\n]"
 * </pre>
 * So we need to additionally check for <tt>deleted</tt>.
 * <p/>
 * There are many ways now to obtain confirmation that a snapshot is deleted: describe-snapshots returns 'deleted';
 * describe-snapshots throwing exception ("not found"); describe-all-snapshots and see that the delete target is not
 * in the list anymore.
 */
public class RdsSnapshotDeletedProgressChecker extends RdsSnapshotProgressChecker implements ProgressChecker<Boolean>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsSnapshotDeletedProgressChecker.class);

  private Boolean result;

  public RdsSnapshotDeletedProgressChecker(String snapshotId,
                                           String logContext,
                                           RdsClient rdsClient, DBSnapshot initialSnapshot)
  {
    super(snapshotId, logContext, rdsClient, initialSnapshot);
  }

  @Override
  public String getDescription()
  {
    return "RDS Delete Snapshot '" + snapshotId + "'";
  }

  /**
   * Checks initial response snapshot.
   */
  @Override
  public void initialCheck()
  {
    super.initialCheck();
    // Does not throw DBSnapshotNotFoundException
  }

  /**
   * Communicates with RDS for updated snapshot progress and checks the status.
   * Concludes if error or if naturally done.
   */
  @Override
  public void followupCheck(int waitNum)
  {
    try
    {
      super.followupCheck(waitNum);
    }
    catch (DBSnapshotNotFoundException e)
    {
      LOGGER.info(getDescription() + " is done");
      done = true;
      result = true;
    }
  }

  /**
   * Checks if the snapshot is in an acceptable intermediate status.
   */
  @Override
  protected void checkSnapshotStatus(DBSnapshot dbSnapshot)
  {
    final String status = dbSnapshot.getStatus();
    final String snapshotId = dbSnapshot.getDBSnapshotIdentifier();
    if (RdsSnapshotStatus.DELETED.equalsString(status))
    {
      LOGGER.info(getDescription() + " is done");
      done = true;
      result = true;
    }
    else if (RdsSnapshotStatus.DELETING.equalsString(status))
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
   * True if the snapshot was fully deleted.  Null if still deleting or timeout.
   */
  @Override
  public Boolean getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns null.
   */
  @Override
  public Boolean timeout()
  {
    LOGGER.error("Snapshot failed to become available prior to timeout");
    return null;
  }
}
