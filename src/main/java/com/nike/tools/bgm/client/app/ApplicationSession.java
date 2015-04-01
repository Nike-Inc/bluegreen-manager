package com.nike.tools.bgm.client.app;

import org.apache.http.Header;
import org.apache.http.client.fluent.Executor;

/**
 * Tracks an authenticated session cookie and fluent http executor for communication with a secure bluegreen application.
 */
public class ApplicationSession
{
  private Executor httpExecutor;
  private Header cookieHeader;

  public ApplicationSession(Executor httpExecutor, Header cookieHeader)
  {
    this.httpExecutor = httpExecutor;
    this.cookieHeader = cookieHeader;
  }

  public Executor getHttpExecutor()
  {
    return httpExecutor;
  }

  public Header getCookieHeader()
  {
    return cookieHeader;
  }
}
