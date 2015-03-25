package com.nike.tools.bgm.client.http;

import java.io.IOException;

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
}
