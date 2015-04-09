package com.nike.tools.bgm.model.domain;

import java.util.ArrayList;
import java.util.List;

public class DatabaseTestHelper
{
  public static final String LIVE_ENV_NAME = "liveEnv";
  public static final String LIVE_LOGICAL_NAME = "lldata";
  public static final String LIVE_PHYSICAL_NAME = "lpdata";

  private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
  private static final String LIVE_URL = "jdbc:mysql://live.hello.com:3306/hellodb?zeroDateTimeBehavior=convertToNull";
  private static final String USERNAME = "dbuser";
  private static final String PASSWORD = "dbpassword";

  /**
   * Makes a fake physical database, referring to a fake logical database, which in turn refers to a fake env.
   * <p/>
   * Does not initialize application vms.
   */
  public static PhysicalDatabase makeFakeLiveDatabase()
  {
    PhysicalDatabase livePhysicalDatabase = new PhysicalDatabase();
    livePhysicalDatabase.setDatabaseType(DatabaseType.RDS);
    livePhysicalDatabase.setInstanceName(LIVE_PHYSICAL_NAME);
    livePhysicalDatabase.setLive(true);
    livePhysicalDatabase.setDriverClassName(DRIVER_CLASS_NAME);
    livePhysicalDatabase.setUrl(LIVE_URL);
    livePhysicalDatabase.setUsername(USERNAME);
    livePhysicalDatabase.setPassword(PASSWORD);

    LogicalDatabase liveLogicalDatabase = new LogicalDatabase();
    liveLogicalDatabase.setLogicalName(LIVE_LOGICAL_NAME);
    liveLogicalDatabase.setPhysicalDatabase(livePhysicalDatabase);
    livePhysicalDatabase.setLogicalDatabase(liveLogicalDatabase);

    Environment liveEnvironment = new Environment();
    liveEnvironment.setEnvName(LIVE_ENV_NAME);
    List<LogicalDatabase> logicalDatabases = new ArrayList<LogicalDatabase>();
    logicalDatabases.add(liveLogicalDatabase);
    liveEnvironment.setLogicalDatabases(logicalDatabases);
    liveLogicalDatabase.setEnvironment(liveEnvironment);

    return livePhysicalDatabase;
  }
}
