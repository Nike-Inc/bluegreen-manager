package bluegreen.manager.client.aws;

/**
 * Represents the possible string values of {@link com.amazonaws.services.rds.model.DBInstance#getDBInstanceStatus}.
 */
public enum RdsInstanceStatus
{
  AVAILABLE,
  BACKING_UP,
  CREATING,
  DELETED,
  DELETING,
  FAILED,
  MODIFYING,
  REBOOTING,
  RESETTING_MASTER_CREDENTIALS,
  STORAGE_FULL,
  INCOMPATIBLE_PARAMETERS,
  INCOMPATIBLE_RESTORE;

  /**
   * True if the given string matches the name of this enum member.  Case-insensitive.
   */
  public boolean equalsString(String str)
  {
    return str == null ? false : name().equals(str.toUpperCase().replace('-', '_'));
  }

  /**
   * Returns name as lowercase and with hyphens, since AWS is apparently doing that.
   */
  @Override
  public String toString()
  {
    return name().toLowerCase().replace('_', '-');
  }
}
