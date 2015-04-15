package com.nike.tools.bgm.model.domain;

import java.util.Arrays;

/**
 * Makes fake entities related to environment, applicationVm, and application.
 */
public class ApplicationTestHelper
{
  private static final String ENV_NAME = "theEnv";
  private static final String SCHEME = "https";
  private static final String VMHOSTNAME = "target-vm.com";
  private static final String HOSTNAME = "target-hostname.com";
  private static final int PORT = 8080;
  private static final String URL_PATH = "/some/resource/path";

  /**
   * Makes a fake application referring to a fake application vm, which in turn refers to a fake env.
   * <p/>
   * Does not initialize logical databases.
   */
  public static Application makeFakeApplication()
  {
    ApplicationVm applicationVm = makeFakeApplicationVm(); //Includes environment.
    Application application = new Application();
    application.setApplicationVm(applicationVm);
    application.setPort(PORT);
    application.setScheme(SCHEME);
    application.setHostname(HOSTNAME);
    application.setUrlPath(URL_PATH);
    applicationVm.setApplications(Arrays.asList(application));
    return application;
  }

  /**
   * Makes an applicationVm pointing to an environment.  No applications.
   */
  public static ApplicationVm makeFakeApplicationVm()
  {
    Environment environment = makeFakeEnvironment();
    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setEnvironment(environment);
    applicationVm.setHostname(VMHOSTNAME);
    environment.setApplicationVms(Arrays.asList(applicationVm));
    return applicationVm;
  }

  /**
   * Makes an environment with a name but nothing else.
   */
  public static Environment makeFakeEnvironment()
  {
    Environment environment = new Environment();
    environment.setEnvName(ENV_NAME);
    return environment;
  }
}
