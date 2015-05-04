package com.nike.tools.bgm.model.domain;

import java.util.Arrays;

/**
 * Makes fake entities related to environment, applicationVm, application, logicalDatabase, physicalDatabase.
 */
public class EnvironmentTestHelper
{
  private static final String[] ENV_NAMES = { "theEnv1", "theEnv2" };
  private static final String APP_SCHEME = "https";
  private static final String[] VM_HOSTNAMES = { "target-vm-1.com", "target-vm-2.com" };
  private static final String[] VM_IPADDRS = { "10.111.222.111", "10.111.222.222" };
  private static final String[] APP_HOSTNAMES = { "target-hostname-1.com", "target-hostname-2.com" };
  private static final int APP_PORT = 8080;
  private static final String APP_URL_PATH = "/some/resource/path";
  private static final String LOGICALDB_NAME = "logicaldb";
  private static final String[] PHYSICALDB_INSTNAMES = { "physDb1", "physDb2" };
  private static final String PHYSICALDB_DRIVER = "com.mysql.jdbc.Driver";
  private static final String[] PHYSICALDB_URLS = { "jdbc:mysql://db-vm-1.com:3306/theDb", "jdbc:mysql://db-vm-2.com:3306/theDb" };
  private static final String PHYSICALDB_USER = "theUser";
  private static final String PHYSICALDB_PASSWORD = "bigSecret";

  private static void checkIndex(int index)
  {
    if (index < 0 || index > 1)
    {
      throw new IllegalArgumentException("Only supporting tests with 1 or 2 environments");
    }
  }

  /**
   * Makes a fake application referring to a fake application vm, which in turn refers to a fake env.
   */
  public static Application makeFakeApplication()
  {
    return makeFakeApplication(0);
  }

  /**
   * Makes a fake application referring to a fake application vm, which in turn refers to a fake env.
   */
  public static Application makeFakeApplication(int index)
  {
    checkIndex(index);
    ApplicationVm applicationVm = makeFakeApplicationVm(index); //Also creates environment.
    Application application = new Application();
    application.setApplicationVm(applicationVm);
    application.setPort(APP_PORT);
    application.setScheme(APP_SCHEME);
    application.setHostname(APP_HOSTNAMES[index]);
    application.setUrlPath(APP_URL_PATH);
    applicationVm.setApplications(Arrays.asList(application));
    return application;
  }

  /**
   * Makes an applicationVm pointing to an environment.  No applications.
   */
  public static ApplicationVm makeFakeApplicationVm()
  {
    return makeFakeApplicationVm(0);
  }

  /**
   * Makes an applicationVm pointing to an environment.  No applications.
   */
  public static ApplicationVm makeFakeApplicationVm(int index)
  {
    checkIndex(index);
    Environment environment = makeFakeEnvironment(index);
    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setEnvironment(environment);
    applicationVm.setHostname(VM_HOSTNAMES[index]);
    applicationVm.setIpAddress(VM_IPADDRS[index]);
    environment.setApplicationVms(Arrays.asList(applicationVm));
    return applicationVm;
  }

  /**
   * Makes an environment with a name but nothing else.
   */
  public static Environment makeFakeEnvironment()
  {
    return makeFakeEnvironment(0);
  }

  /**
   * Makes an environment with a name but nothing else.
   */
  public static Environment makeFakeEnvironment(int index)
  {
    checkIndex(index);
    Environment environment = new Environment();
    environment.setEnvName(ENV_NAMES[index]);
    return environment;
  }

  /**
   * Makes an environment fully stocked with an application vm, an application, logicaldb, physicaldb.
   */
  public static Environment makeFakeFullEnvironment(int index)
  {
    checkIndex(index);
    Application application = makeFakeApplication(index);
    Environment environment = application.getApplicationVm().getEnvironment();
    makeFakePhysicalDatabase(index, environment);
    return environment;
  }

  /**
   * Makes a fake physicaldb referring to a fake logicaldb, which in turn refers to the specified env.
   */
  public static PhysicalDatabase makeFakePhysicalDatabase(int index, Environment environment)
  {
    checkIndex(index);
    LogicalDatabase logicalDatabase = makeFakeLogicalDatabase(index, environment);
    PhysicalDatabase physicalDatabase = new PhysicalDatabase();
    physicalDatabase.setLogicalDatabase(logicalDatabase);
    physicalDatabase.setDatabaseType(DatabaseType.RDS);
    physicalDatabase.setInstanceName(PHYSICALDB_INSTNAMES[index]);
    physicalDatabase.setLive(index == 0);
    physicalDatabase.setDriverClassName(PHYSICALDB_DRIVER);
    physicalDatabase.setUrl(PHYSICALDB_URLS[index]);
    physicalDatabase.setUsername(PHYSICALDB_USER);
    physicalDatabase.setPassword(PHYSICALDB_PASSWORD);
    logicalDatabase.setPhysicalDatabase(physicalDatabase);
    return physicalDatabase;
  }

  /**
   * Makes a fake logicaldb, which refers to the specified env.  No physicaldb.
   */
  public static LogicalDatabase makeFakeLogicalDatabase(int index, Environment environment)
  {
    checkIndex(index);
    LogicalDatabase logicalDatabase = new LogicalDatabase();
    logicalDatabase.setEnvironment(environment);
    logicalDatabase.setLogicalName(LOGICALDB_NAME);
    environment.addLogicalDatabase(logicalDatabase);
    return logicalDatabase;
  }

}
