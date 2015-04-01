package com.nike.tools.bgm.client.http;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

/**
 * Httpcomponents helpers.
 * <p/>
 * Converts IOException to RuntimeException.
 */
@Component
public class HttpHelper
{
  public static final String HEADERNAME_SET_COOKIE = "Set-Cookie";

  /**
   * Posts the given authentication-oriented content body to the given uri and returns the response cookie.
   */
  public Header postForCookie(Executor executor, String uri, String contentString, ContentType contentType)
  {
    try
    {
      HttpResponse httpResponse = executor.execute(Request.Post(uri).bodyString(contentString, contentType)).returnResponse();
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      Header cookieHeader = null;
      String body = null;
      if (200 <= statusCode && statusCode < 400)
      {
        cookieHeader = httpResponse.getFirstHeader(HEADERNAME_SET_COOKIE);
        if (cookieHeader != null && StringUtils.isNotBlank(cookieHeader.getValue()))
        {
          body = EntityUtils.toString(httpResponse.getEntity());
          if (StringUtils.equals("true", body))
          {
            return cookieHeader;
          }
        }
      }
      throw new RuntimeException("Failed to obtain cookie from uri " + uri + ", statusCode: " + statusCode
          + ", cookieHeader: " + cookieHeader + ", body: " + body);
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri + ", contentType: '" + contentType + "', contentString: <" + contentString + ">", e);
    }
  }

  /**
   * PUTs a uri (no content body) in an existing session, returns the response body as a string.
   */
  public String executePut(final Executor executor, Header cookieHeader, final String uri)
  {
    try
    {
      return executor.execute(Request.Put(uri).addHeader(cookieHeader)).returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("PUT uri: " + uri + ", cookie " + cookieToString(cookieHeader), e);
    }
  }

  /**
   * GETs a uri in an existing session, returns the response body as a string.
   */
  public String executeGet(Executor executor, Header cookieHeader, String uri)
  {
    try
    {
      return executor.execute(Request.Get(uri).addHeader(cookieHeader)).returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("GET uri: " + uri + ", cookie " + cookieToString(cookieHeader), e);
    }
  }

  /**
   * Makes a printable version of the cookie header.
   */
  private String cookieToString(Header cookieHeader)
  {
    if (cookieHeader == null)
    {
      return "null";
    }
    else
    {
      return cookieHeader.getName() + ": " + cookieHeader.getValue();
    }
  }
}
