package com.nike.tools.bgm.client.http;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Component;

/**
 * Provides the most commonly needed GET/POST methods on top of httpcomponents.
 */
@Component
public class HttpHelper
{
  public String executePost(final String uri)
  {
    try
    {
      return Request.Post(uri).execute().returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri, e);
    }
  }

  public String executePost(final String uri, final String contentString, final ContentType contentType)
  {
    try
    {
      return Request.Post(uri).bodyString(contentString, contentType).execute().returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri + ", contentType: '" + contentType + "', contentString: <" + contentString + ">", e);
    }
  }

  public String executePost(final Executor executor, final String uri)
  {
    try
    {
      return executor.execute(Request.Post(uri)).returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri, e);
    }
  }

  public String executePost(final Executor executor,
                            final String uri,
                            final String contentString,
                            final ContentType contentType)
  {
    try
    {
      return executor.execute(Request.Post(uri).bodyString(contentString, contentType)).returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri + ", contentType: '" + contentType + "', contentString: <" + contentString + ">", e);
    }
  }

  /**
   * Posts to the given uri and returns the response cookie.
   */
  public Header postForCookie(Executor executor, String uri, String contentString, ContentType contentType)
  {
    try
    {
      HttpResponse httpResponse = executor.execute(Request.Post(uri).bodyString(contentString, contentType)).returnResponse();
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      Header cookieHeader = httpResponse.getFirstHeader("Set-Cookie");
      if (200 <= statusCode && statusCode < 400 && cookieHeader != null && StringUtils.isNotBlank(cookieHeader.getValue()))
      {
        return cookieHeader;
      }
      else
      {
        throw new RuntimeException("Failed to obtain cookie from uri " + uri + ", statusCode: " + statusCode + ", cookieHeader: " + cookieHeader);
      }
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri + ", contentType: '" + contentType + "', contentString: <" + contentString + ">", e);
    }
  }

  public String executePut(final Executor executor, final String uri)
  {
    try
    {
      return executor.execute(Request.Put(uri)).returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("PUT uri: " + uri, e);
    }
  }

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

  public String executeGet(final String uri)
  {
    try
    {
      return Request.Get(uri).execute().returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("GET uri: " + uri, e);
    }
  }

  public String executeGet(final Executor executor, final String uri)
  {
    try
    {
      return executor.execute(Request.Get(uri)).returnContent().toString();
    }
    catch (IOException e)
    {
      throw new RuntimeException("GET uri: " + uri, e);
    }
  }

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
