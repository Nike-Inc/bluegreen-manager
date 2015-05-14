package com.nike.tools.bgm.client.aws;

/**
 * Represents the possible string values of {@link com.amazonaws.services.rds.model.DBParameterGroupStatus#getParameterApplyStatus}.
 */
public enum RdsParameterApplyStatus
{
  APPLYING,
  PENDING_REBOOT,
  IN_SYNC;

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

  /**
   * Parses a string into an enum value.
   */
  public static RdsParameterApplyStatus fromString(String str)
  {
    if (str != null)
    {
      for (RdsParameterApplyStatus status : values())
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
