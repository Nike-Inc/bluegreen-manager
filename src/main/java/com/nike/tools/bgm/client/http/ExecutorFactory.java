package com.nike.tools.bgm.client.http;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.Application;

/**
 * Httpcomponents-related factory that can create an authenticated executor for fluent requests to an application.
 * <p/>
 * Assumption currently is that all target applications will support the same bluegreen system user.
 */
@Component
public class ExecutorFactory
{
  @Autowired
  private HttpClient httpClient;

  @Autowired
  private ExecutorFactoryFactory executorFactoryFactory;

  @Value("${bluegreen.application.username}")
  private String applicationUsername;

  @Value("${bluegreen.application.password}")
  private String applicationPassword;

  /**
   * Makes an http executor which is authenticated to the given application.
   * <p/>
   * Does not actually require application urlPath, only scheme://hostname:port.
   * <p/>
   * The underlying httpClient is thread-safe but in fluent-hc 4.3.1 the executor is not.
   * We're not yet using the executor fix of 4.3.2, see https://issues.apache.org/jira/browse/HTTPCLIENT-1437
   */
  public Executor makeAuthenticatedExecutor(Application application)
  {
    Executor executor = executorFactoryFactory.newInstance(httpClient);
    String hostname = application.getApplicationVm().getHostname();
    int port = application.getPort();
    String scheme = application.getScheme();
    HttpHost httpHost = new HttpHost(hostname, port, scheme);
    executor.auth(httpHost, applicationUsername, applicationPassword).authPreemptive(httpHost);
    return executor;
  }

  // For test purposes only
  public void setApplicationPassword(String applicationPassword)
  {
    this.applicationPassword = applicationPassword;
  }

  // For test purposes only
  public void setApplicationUsername(String applicationUsername)
  {
    this.applicationUsername = applicationUsername;
  }
}
