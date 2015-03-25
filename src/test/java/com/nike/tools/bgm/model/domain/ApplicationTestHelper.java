package com.nike.tools.bgm.model.domain;

public class ApplicationTestHelper
{
  private static final String SCHEME = "https";
  private static final String HOSTNAME = "target-hostname.com";
  private static final int PORT = 8080;
  private static final String URL_PATH = "/some/resource/path";

  /**
   * Makes a fake application referring to a fake application vm.
   */
  public static Application makeFakeApplication()
  {
    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setHostname(HOSTNAME);
    Application application = new Application();
    application.setApplicationVm(applicationVm);
    application.setPort(PORT);
    application.setScheme(SCHEME);
    application.setUrlPath(URL_PATH);
    return application;
  }
}
