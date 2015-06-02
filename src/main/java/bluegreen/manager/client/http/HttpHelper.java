package bluegreen.manager.client.http;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import bluegreen.manager.client.app.LoginResult;

/**
 * Httpcomponents helpers.
 * <p/>
 * Converts IOException to RuntimeException.
 */
@Component
public class HttpHelper
{
  public static final String HEADERNAME_SET_COOKIE = "Set-Cookie";

  @Autowired
  private Gson gson;

  //Test purposes only
  void setGson(Gson gson)
  {
    this.gson = gson;
  }

  /**
   * Posts the authentication parameters to the given uri and validates the response cookie.
   * Returns silently if successful, else throws.
   */
  public void postAuthForCookie(Executor executor, String uri, NameValuePair[] authParams)
  {
    try
    {
      Request request = Request.Post(uri).bodyForm(authParams);
      HttpResponse httpResponse = executor.execute(request).returnResponse();
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      Header cookieHeader = null;
      String body = null;
      if (200 <= statusCode && statusCode < 400)
      {
        cookieHeader = httpResponse.getFirstHeader(HEADERNAME_SET_COOKIE);
        if (cookieHeader != null && StringUtils.isNotBlank(cookieHeader.getValue()))
        {
          body = EntityUtils.toString(httpResponse.getEntity());
          LoginResult result = gson.fromJson(body, LoginResult.class);
          if (result != null && result.isLoggedIn())
          {
            return; //success
          }
        }
      }
      throw new RuntimeException("Failed to obtain response cookie from uri " + uri + ", statusCode: " + statusCode
          + ", cookieHeader: " + cookieToString(cookieHeader) + ", body: " + body);
      //Note: if cookieStore already has valid cookie then response won't return a new cookie
    }
    catch (IOException e)
    {
      throw new RuntimeException("POST uri: " + uri + ", authParams", e);
    }
  }

  /**
   * PUTs a uri (no content body) in an existing session, returns the response body as a string.
   */
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

  /**
   * GETs a uri in an existing session, returns the response body as a string.
   */
  public String executeGet(Executor executor, String uri)
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
