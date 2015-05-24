package bluegreen.manager.client.aws;

/**
 * Identifies an rds snapshot based on the position of its corresponding physical database entity in the
 * bluegreen datamodel.
 * <p/>
 * When bluegreen stagingDeploy makes a snapshot of the physicaldb, it will use this id.
 */
public class RdsSnapshotBluegreenId
{
  /**
   * Using '9' as a delimiter char, since the only other special char allowed in a snapshotId is '-' and we are
   * already using that inside the tokens we use to make the id.
   */
  private static final char SNAPSHOT_ID_DELIMITER = '9';
  private static final String SNAPSHOT_PREFIX = "bluegreen";

  private String envName;
  private String logicalDatabaseName;
  private String physicalDatabaseInstanceName;

  public RdsSnapshotBluegreenId(String envName, String logicalDatabaseName, String physicalDatabaseInstanceName)
  {
    this.envName = envName;
    this.logicalDatabaseName = logicalDatabaseName;
    this.physicalDatabaseInstanceName = physicalDatabaseInstanceName;
  }

  /**
   * Makes an RDS snapshot id based on the live physicaldb.  Always the same string for a given physicaldb.
   * <p/>
   * If we didn't specify one, Amazon would create a random identifier for us.
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(SNAPSHOT_PREFIX);
    sb.append(SNAPSHOT_ID_DELIMITER);
    sb.append(envName);
    sb.append(SNAPSHOT_ID_DELIMITER);
    sb.append(logicalDatabaseName);
    sb.append(SNAPSHOT_ID_DELIMITER);
    sb.append(physicalDatabaseInstanceName);
    return sb.toString();
  }

}
