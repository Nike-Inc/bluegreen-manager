package com.nike.tools.bgm.client.aws;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the return type of several AWS api methods that officially return String but in practice
 * return one of three fixed values (creating, available, deleting).
 * <p/>
 * So far is applicable to:
 * <ul>
 * <li>{@link com.amazonaws.services.rds.model.DBSnapshot#getStatus}</li>
 * <li>{@link com.amazonaws.services.rds.model.DBInstance#getDBInstanceStatus}</li>
 * </ul>
 */
public enum AvailableStatus
{
  CREATING,
  AVAILABLE,
  DELETING;

  /**
   * True if the given string matches the name of this enum member.  Case-insensitive.
   */
  public boolean equalsString(String str)
  {
    return StringUtils.equalsIgnoreCase(name(), str);
  }

  /**
   * Returns name as lowercase, since AWS is apparently doing that.
   */
  @Override
  public String toString()
  {
    return name().toLowerCase();
  }
}
