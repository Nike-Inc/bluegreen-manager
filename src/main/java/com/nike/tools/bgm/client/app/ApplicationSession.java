package com.nike.tools.bgm.client.app;

import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;

/**
 * Tracks a fluent http executor and its cookieStore for communication with a secure bluegreen application.
 */
public class ApplicationSession
{
  private Executor httpExecutor;
  private CookieStore cookieStore;

  public ApplicationSession(Executor httpExecutor, CookieStore cookieStore)
  {
    this.httpExecutor = httpExecutor;
    this.cookieStore = cookieStore;
  }

  public Executor getHttpExecutor()
  {
    return httpExecutor;
  }

  public CookieStore getCookieStore()
  {
    return cookieStore;
  }
}
