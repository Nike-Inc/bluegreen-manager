package com.nike.tools.bgm.client.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.nike.tools.bgm.client.http.HttpHelper.HEADERNAME_SET_COOKIE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpHelperTest
{
  private static final String URI = "https://the-server.com/rest/service";
  private static final String COOKIE_VALUE = "someValue; Path=/";
  private static final String JSON_RESPONSE_CONTENT = "{'value':'hello'}";

  @Mock
  private Executor mockExecutor;

  @Mock
  private Response mockResponse;

  @Mock
  private Content mockContent;

  private HttpHelper httpHelper = new HttpHelper();

  @Before
  public void setUp() throws IOException
  {
    when(mockExecutor.execute(any(Request.class))).thenReturn(mockResponse);
    when(mockResponse.returnContent()).thenReturn(mockContent);
    when(mockContent.toString()).thenReturn(JSON_RESPONSE_CONTENT);
  }

  /**
   * Tests the authentication post.
   */
  private Header testPostAuthForCookie(int responseStatus, String headerName, boolean isValidUser) throws IOException
  {
    HttpResponse fakeHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, responseStatus, "some reason");
    fakeHttpResponse.addHeader(headerName, COOKIE_VALUE);
    fakeHttpResponse.setEntity(new StringEntity("" + isValidUser));
    when(mockResponse.returnResponse()).thenReturn(fakeHttpResponse);
    NameValuePair[] authParams = new NameValuePair[] {
        new BasicNameValuePair("auth1", "hello"),
        new BasicNameValuePair("auth2", "world")
    };

    return httpHelper.postAuthForCookie(mockExecutor, URI, authParams);
  }

  /**
   * Tests the case of a successful authentication.
   */
  @Test
  public void testPostAuthForCookie_Pass() throws IOException
  {
    Header cookieHeader = testPostAuthForCookie(HttpStatus.SC_OK, HEADERNAME_SET_COOKIE, true);

    assertEquals(COOKIE_VALUE, cookieHeader.getValue());
  }

  /**
   * Post would succeeed except for non-ok return status code.
   */
  @Test(expected = RuntimeException.class)
  public void testPostAuthForCookie_BadStatus() throws IOException
  {
    testPostAuthForCookie(HttpStatus.SC_UNAUTHORIZED, HEADERNAME_SET_COOKIE, true);
  }

  /**
   * Post would succeeed except for missing cookie response header.
   */
  @Test(expected = RuntimeException.class)
  public void testPostAuthForCookie_MissingCookie() throws IOException
  {
    testPostAuthForCookie(HttpStatus.SC_OK, "Some-Other-Header", true);
  }

  /**
   * Post would succeeed except for unexpected body response entity.
   */
  @Test(expected = RuntimeException.class)
  public void testPostAuthForCookie_InvalidUser() throws IOException
  {
    testPostAuthForCookie(HttpStatus.SC_OK, HEADERNAME_SET_COOKIE, false);
  }

  /**
   * Post fails with an IOException during http execution.
   */
  @Test(expected = RuntimeException.class)
  public void testPostAuthForCookie_ExecuteThrows() throws IOException
  {
    reset(mockExecutor);
    when(mockExecutor.execute(any(Request.class))).thenThrow(IOException.class);
    testPostAuthForCookie(HttpStatus.SC_OK, HEADERNAME_SET_COOKIE, true);
  }

  /**
   * Tests the putter.  Would be better if we could assert that the cookie gets used.
   */
  @Test
  public void testExecutePut_Pass()
  {
    Header cookieHeader = new BasicHeader(HEADERNAME_SET_COOKIE, COOKIE_VALUE);
    assertEquals(JSON_RESPONSE_CONTENT, httpHelper.executePut(mockExecutor, cookieHeader, URI));
  }

  /**
   * Tests the putter.  Would be better if we could assert that the cookie gets used.
   */
  @Test
  public void testExecuteGet_Pass()
  {
    Header cookieHeader = new BasicHeader(HEADERNAME_SET_COOKIE, COOKIE_VALUE);
    assertEquals(JSON_RESPONSE_CONTENT, httpHelper.executeGet(mockExecutor, cookieHeader, URI));
  }
}