package bluegreen.manager.client.app;

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

  public PhysicalDatabase()
  {
  }

  public PhysicalDatabase(String envName, String logicalName, String dbUrl, String dbUsername, boolean dbIsLive)
  {
    this.envName = envName;
    this.logicalName = logicalName;
    this.dbUrl = dbUrl;
    this.dbUsername = dbUsername;
    this.dbIsLive = dbIsLive;
  }

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

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("PhysicalDatabase[");
    sb.append("envName: ");
    sb.append(envName);
    sb.append(", logicalName: ");
    sb.append(logicalName);
    sb.append(", dbUrl: ");
    sb.append(dbUrl);
    sb.append(", dbUsername: ");
    sb.append(dbUsername);
    sb.append(", dbIsLive: ");
    sb.append(dbIsLive);
    sb.append("]");
    return sb.toString();
  }
}
