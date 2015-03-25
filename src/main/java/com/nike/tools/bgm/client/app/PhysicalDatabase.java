package com.nike.tools.bgm.client.app;

/**
 * Represents a physical database as known to a bluegreen application.
 * <p/>
 * (Not to be confused with the bluegreen manager's data model entity.)
 */
public class PhysicalDatabase
{
  private String envName;

  private String logicalName;

  private String dbUrl;

  private String dbUsername;

  private boolean dbIsLive;

  public String getEnvName()
  {
    return envName;
  }

  public void setEnvName(String envName)
  {
    this.envName = envName;
  }

  public String getLogicalName()
  {
    return logicalName;
  }

  public void setLogicalName(String logicalName)
  {
    this.logicalName = logicalName;
  }

  public String getDbUrl()
  {
    return dbUrl;
  }

  public void setDbUrl(String dbUrl)
  {
    this.dbUrl = dbUrl;
  }

  public String getDbUsername()
  {
    return dbUsername;
  }

  public void setDbUsername(String dbUsername)
  {
    this.dbUsername = dbUsername;
  }

  public boolean isDbIsLive()
  {
    return dbIsLive;
  }

  public void setDbIsLive(boolean dbIsLive)
  {
    this.dbIsLive = dbIsLive;
  }
}
