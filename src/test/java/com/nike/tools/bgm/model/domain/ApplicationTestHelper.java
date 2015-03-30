package com.nike.tools.bgm.model.domain;

import java.util.Arrays;

public class ApplicationTestHelper
{
  private static final String ENV_NAME = "theEnv";
  private static final String SCHEME = "https";
  private static final String VMHOSTNAME = "target-vm.com";
  private static final String HOSTNAME = "target-hostname.com";
  private static final int PORT = 8080;
  private static final String URL_PATH = "/some/resource/path";

  /**
   * Makes a fake application referring to a fake application vm, which in turn refers to a fake env..
   */
  public static Application makeFakeApplication()
  {
    Environment environment = new Environment();
    environment.setEnvName(ENV_NAME);
    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setEnvironment(environment);
    applicationVm.setHostname(VMHOSTNAME);
    Application application = new Application();
    application.setApplicationVm(applicationVm);
    application.setPort(PORT);
    application.setScheme(SCHEME);
    application.setHostname(HOSTNAME);
    application.setUrlPath(URL_PATH);
    environment.setApplicationVms(Arrays.asList(applicationVm));
    applicationVm.setApplications(Arrays.asList(application));
    return application;
  }
}
