package com.nike.tools.bgm.client.aws;

/**
 * Represents the possible string values of {@link com.amazonaws.services.rds.model.DBSnapshot#getStatus}.
 */
public enum SnapshotStatus
{
  CREATING,
  AVAILABLE,
  DELETING;

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
}
