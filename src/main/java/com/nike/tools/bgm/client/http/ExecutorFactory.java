package com.nike.tools.bgm.client.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Httpcomponents-related factory that can create an executor on top of an httpClient object.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class ExecutorFactory
{
  @Autowired
  private HttpClient httpClient;

  /**
   * Makes an http executor on top of the common httpClient object.
   * <p/>
   * The underlying httpClient is thread-safe but in fluent-hc 4.3.1 the executor is not.
   * We're not yet using the executor fix of 4.3.2, see https://issues.apache.org/jira/browse/HTTPCLIENT-1437
   */
  public Executor makeExecutor()
  {
    return Executor.newInstance(httpClient);
  }

}
