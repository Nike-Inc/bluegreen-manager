package com.nike.tools.bgm.client.aws;

/**
 * Represents the possible string values of {@link com.amazonaws.services.rds.model.DBSnapshot#getStatus}.
 */
public enum RdsSnapshotStatus
{
  /*
  http://docs.aws.amazon.com/AmazonRDS/latest/CommandLineReference/CLIReference-cmd-DescribeDBSnapshots.html says
  the only snapshot status values are <tt>creating | available | deleting</tt> ...but they lie, look at this result:

DEBUG org.apache.http.impl.conn.Wire.wire(72) -  << "  <DeleteDBSnapshotResult>[\n]"
DEBUG org.apache.http.impl.conn.Wire.wire(72) -  << "    <DBSnapshot>[\n]"
DEBUG org.apache.http.impl.conn.Wire.wire(72) -  << "      <Status>deleted</Status>[\n]"

   */

  CREATING,
  AVAILABLE,
  DELETING,
  DELETED;

  /**
   * True if the given string matches the name of this enum member.  Case-insensitive.
   */
  public boolean equalsString(String str)
  {
    return str == null ? false : name().equals(str.toUpperCase());
  }

  /**
   * Returns name as lowercase, since AWS is apparently doing that.
   */
  @Override
  public String toString()
  {
    return name().toLowerCase();
  }

  /**
   * Parses a string into an enum value.
   */
  public static RdsSnapshotStatus fromString(String str)
  {
    if (str != null)
    {
      for (RdsSnapshotStatus status : values())
      {
        if (status.equalsString(str))
        {
          return status;
        }
      }
    }
    return null;
  }
}
