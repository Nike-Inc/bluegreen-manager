package com.nike.tools.bgm.client.app;

import org.apache.http.Header;
import org.apache.http.client.fluent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nike.tools.bgm.client.http.ExecutorFactory;
import com.nike.tools.bgm.client.http.HttpHelper;
import com.nike.tools.bgm.client.http.HttpMethodType;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationTestHelper;
import com.nike.tools.bgm.utils.ThreadSleeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationClientTest
{
  private static final String JSON_FAKE_LOCKABLE_LOCKED = "{'lockError': true}";
  private static final String JSON_FAKE_LOCKABLE_NOT_LOCKED = "{'lockError': false}";
  private static final String JSON_DB_FREEZE_PROGRESS = "{'mode':{'printable':'Normal','transition':'blah','code':'NORMAL'}, 'username':'charlie', 'startTime':'12pm', 'endTime':'1pm', 'scannersAwaitingTermination':null, 'lockError':false, 'transitionError':null}";
  private static final String JSON_DISCOVERY_RESULT = "{'physicalDatabase':{'envName':'env1', 'logicalName':'hello', 'dbUrl':'theUrl', 'dbUsername':'user', 'dbIsLive':true}, 'lockError':false, 'discoveryError':null}";
  private static final String TEST_URI = "http://helloworld.com:8080/restful/interface";
  private static final String METHOD_PATH = "someResourceService";
  private static final Application FAKE_APPLICATION = ApplicationTestHelper.makeFakeApplication();
  private static final String FAKE_APP_URI = FAKE_APPLICATION.makeHostnameUri() + "/" + METHOD_PATH;

  @InjectMocks
  private ApplicationClient applicationClient;

  @Mock
  private ExecutorFactory mockExecutorFactory;

  @Mock
  private HttpHelper mockHttpHelper;

  private Gson gson; //Final class, mockito cannot mock

  @Mock
  private ThreadSleeper mockThreadSleeper;

  @Mock
  private Executor mockExecutor;

  @Mock
  private Header mockCookieHeader;

  private ApplicationSession fakeSession;

  @Before
  public void setUp()
  {
    GsonFactory gsonFactory = new GsonFactory();
    gsonFactory.setGsonBuilder(new GsonBuilder());
    gson = gsonFactory.makeGson();
    applicationClient.setGson(gson);
    fakeSession = new ApplicationSession(mockExecutor, mockCookieHeader);
    when(mockExecutorFactory.makeExecutor()).thenReturn(mockExecutor);
  }

  /**
   * Tests that a null http response body turns into a null Lockable object.
   */
  @Test
  public void testTryRequest_NullResponse()
  {
    when(mockHttpHelper.executeGet(mockExecutor, mockCookieHeader, TEST_URI)).thenReturn(null);

    Lockable response = applicationClient.tryRequest(HttpMethodType.GET, fakeSession, TEST_URI, FakeLockable.class, 0);

    assertNull(response);
    verify(mockHttpHelper).executeGet(mockExecutor, mockCookieHeader, TEST_URI);
  }

  /**
   * Tests the case of a response claiming lock error.
   */
  @Test
  public void testTryRequest_LockError()
  {
    when(mockHttpHelper.executePut(mockExecutor, mockCookieHeader, TEST_URI)).thenReturn(JSON_FAKE_LOCKABLE_LOCKED);

    Lockable response = applicationClient.tryRequest(HttpMethodType.PUT, fakeSession, TEST_URI, FakeLockable.class, 0);

    assertTrue(response.isLockError());
    verify(mockHttpHelper).executePut(mockExecutor, mockCookieHeader, TEST_URI);
  }

  /**
   * Tests the case of a successful response.
   */
  @Test
  public void testTryRequest_Success()
  {
    when(mockHttpHelper.executeGet(mockExecutor, mockCookieHeader, TEST_URI)).thenReturn(JSON_FAKE_LOCKABLE_NOT_LOCKED);

    Lockable response = applicationClient.tryRequest(HttpMethodType.GET, fakeSession, TEST_URI, FakeLockable.class, 0);

    assertFalse(response.isLockError());
    verify(mockHttpHelper).executeGet(mockExecutor, mockCookieHeader, TEST_URI);
  }

  /**
   * Tests the case where the request works the first time.
   */
  @Test
  public void testRequestWithRetry_OkFirstTry()
  {
    when(mockHttpHelper.executeGet(mockExecutor, mockCookieHeader, FAKE_APP_URI)).thenReturn(JSON_FAKE_LOCKABLE_NOT_LOCKED);

    Lockable response = applicationClient.requestWithRetry(FAKE_APPLICATION, fakeSession, HttpMethodType.GET, METHOD_PATH, FakeLockable.class);

    assertFalse(response.isLockError());
    verify(mockHttpHelper).executeGet(mockExecutor, mockCookieHeader, FAKE_APP_URI);
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Tests the case where the request fails twice then works the third time.
   */
  @Test
  public void testRequestWithRetry_OkThirdTry() throws InterruptedException
  {
    assertTrue("Test requirement", 3 >= ApplicationClient.MAX_NUM_TRIES);

    when(mockHttpHelper.executeGet(mockExecutor, mockCookieHeader, FAKE_APP_URI))
        .thenReturn(null)
        .thenReturn(JSON_FAKE_LOCKABLE_LOCKED)
        .thenReturn(JSON_FAKE_LOCKABLE_NOT_LOCKED);

    Lockable response = applicationClient.requestWithRetry(FAKE_APPLICATION, fakeSession, HttpMethodType.GET, METHOD_PATH, FakeLockable.class);

    assertFalse(response.isLockError());
    verify(mockHttpHelper, times(3)).executeGet(mockExecutor, mockCookieHeader, FAKE_APP_URI);
    verify(mockThreadSleeper, times(2)).sleep(anyLong());
  }

  /**
   * Tests the case where the request completely fails (always returns lock error).
   */
  @Test
  public void testRequestWithRetry_AlwaysLocked() throws InterruptedException
  {
    when(mockHttpHelper.executePut(mockExecutor, mockCookieHeader, FAKE_APP_URI)).thenReturn(JSON_FAKE_LOCKABLE_LOCKED);

    Lockable response = applicationClient.requestWithRetry(FAKE_APPLICATION, fakeSession, HttpMethodType.PUT, METHOD_PATH, FakeLockable.class);

    assertTrue(response.isLockError());
    verify(mockHttpHelper, times(ApplicationClient.MAX_NUM_TRIES)).executePut(mockExecutor, mockCookieHeader, FAKE_APP_URI);
    verify(mockThreadSleeper, times(ApplicationClient.MAX_NUM_TRIES - 1)).sleep(anyLong());
  }

  /**
   * Tests a successful call to the get-progress resource.
   */
  @Test
  public void testGetDbFreezeProgress()
  {
    when(mockHttpHelper.executeGet(eq(mockExecutor), eq(mockCookieHeader), anyString())).thenReturn(JSON_DB_FREEZE_PROGRESS);

    DbFreezeProgress response = applicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession);

    assertFalse(response.isLockError());
    assertEquals(DbFreezeMode.NORMAL, response.getMode());
    verify(mockHttpHelper).executeGet(eq(mockExecutor), eq(mockCookieHeader), anyString());
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Tests a successful call to the enter-freeze resource.
   */
  @Test
  public void testPutEnterDbFreeze()
  {
    when(mockHttpHelper.executePut(eq(mockExecutor), eq(mockCookieHeader), anyString())).thenReturn(JSON_DB_FREEZE_PROGRESS);

    DbFreezeProgress response = applicationClient.putEnterDbFreeze(FAKE_APPLICATION, fakeSession);

    assertFalse(response.isLockError());
    assertEquals(DbFreezeMode.NORMAL, response.getMode());
    verify(mockHttpHelper).executePut(eq(mockExecutor), eq(mockCookieHeader), anyString());
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Tests a successful call to the enter-freeze resource.
   */
  @Test
  public void testPutDiscoverDb()
  {
    when(mockExecutorFactory.makeExecutor()).thenReturn(mockExecutor);
    when(mockHttpHelper.executePut(eq(mockExecutor), eq(mockCookieHeader), anyString())).thenReturn(JSON_DISCOVERY_RESULT);

    DiscoveryResult response = applicationClient.putDiscoverDb(FAKE_APPLICATION, fakeSession);

    assertFalse(response.isLockError());
    assertEquals("env1", response.getPhysicalDatabase().getEnvName());
    verify(mockHttpHelper).executePut(eq(mockExecutor), eq(mockCookieHeader), anyString());
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Simpleminded uri test.
   */
  @Test
  public void testMakeHostnameUri()
  {
    assertTrue(FAKE_APPLICATION.makeHostnameUri().startsWith("http"));
  }

  /**
   * Tests the ability to make an alternate uri based on the application's host.
   */
  @Test
  public void testMakeAlternateUri()
  {
    String uri = FAKE_APPLICATION.makeAlternateUri("alternate");
    assertTrue(uri.startsWith("http"));
    assertTrue(uri.endsWith("alternate"));
  }

  /**
   * For client tests that don't need to care about concrete lockable implementations.
   */
  private static class FakeLockable implements Lockable
  {

    private boolean lockError;

    @Override
    public boolean isLockError()
    {
      return lockError;
    }

    @Override
    public String toString()
    {
      return "FakeLockable[lockError: " + lockError + "]";
    }
  }
}