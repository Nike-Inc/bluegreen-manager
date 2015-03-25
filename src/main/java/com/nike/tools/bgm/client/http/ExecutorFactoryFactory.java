package com.nike.tools.bgm.client.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.springframework.stereotype.Component;

/**
 * This kind of nesting is unfortunate but is the only way to make ExecutorFactory testable.
 */
@Component
public class ExecutorFactoryFactory
{
  /**
   * Returns a new executor instance.
   */
  public Executor newInstance(HttpClient httpClient)
  {
    return Executor.newInstance(httpClient);
  }
}
