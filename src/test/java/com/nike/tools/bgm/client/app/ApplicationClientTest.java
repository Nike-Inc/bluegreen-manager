package com.nike.tools.bgm.client.app;

import org.apache.http.client.fluent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.nike.tools.bgm.client.http.ExecutorFactory;
import com.nike.tools.bgm.client.http.HttpHelper;
import com.nike.tools.bgm.client.http.HttpMethodType;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationTestHelper;
import com.nike.tools.bgm.utils.ThreadSleeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationClientTest
{
  private static final String JSON_FAKE_LOCKABLE_LOCKED = "{'lockError': true}";
  private static final String JSON_FAKE_LOCKABLE_NOT_LOCKED = "{'lockError': false}";
  private static final String JSON_DB_FREEZE_PROGRESS = "{'mode':'NORMAL', 'username':'charlie', 'startTime':'12pm', 'endTime':'1pm', 'scannersAwaitingTermination':null, 'lockError':false, 'transitionError':null}";
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

  private Gson gson = new Gson(); //Final class, mockito cannot mock

  @Mock
  private ThreadSleeper mockThreadSleeper;

  @Before
  public void setUp()
  {
    applicationClient.setGson(gson);
  }

  /**
   * Tests that the client will reuse an executor for the same application.
   */
  @Test
  public void testMakeOrReuseAuthenticatedExecutor()
  {
    Application app1 = new Application();
    Application app2 = new Application();
    Executor mockExecutor1 = mock(Executor.class);
    Executor mockExecutor2 = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(app1)).thenReturn(mockExecutor1);
    when(mockExecutorFactory.makeAuthenticatedExecutor(app2)).thenReturn(mockExecutor2);

    for (int idx = 0; idx < 2; ++idx)
    {
      assertSame(mockExecutor1, applicationClient.makeOrReuseAuthenticatedExecutor(app1));
      assertSame(mockExecutor2, applicationClient.makeOrReuseAuthenticatedExecutor(app2));
    }

    verify(mockExecutorFactory).makeAuthenticatedExecutor(app1);
    verify(mockExecutorFactory).makeAuthenticatedExecutor(app2);
  }

  /**
   * Tests that a null http response body turns into a null Lockable object.
   */
  @Test
  public void testTryRequest_NullResponse()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockHttpHelper.executeGet(mockExecutor, TEST_URI)).thenReturn(null);

    Lockable response = applicationClient.tryRequest(HttpMethodType.GET, mockExecutor, TEST_URI, FakeLockable.class, 0);

    assertNull(response);
    verify(mockHttpHelper).executeGet(mockExecutor, TEST_URI);
  }

  /**
   * Tests the case of a response claiming lock error.
   */
  @Test
  public void testTryRequest_LockError()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockHttpHelper.executePut(mockExecutor, TEST_URI)).thenReturn(JSON_FAKE_LOCKABLE_LOCKED);

    Lockable response = applicationClient.tryRequest(HttpMethodType.PUT, mockExecutor, TEST_URI, FakeLockable.class, 0);

    assertTrue(response.isLockError());
    verify(mockHttpHelper).executePut(mockExecutor, TEST_URI);
  }

  /**
   * Tests the case of a successful response.
   */
  @Test
  public void testTryRequest_Success()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockHttpHelper.executeGet(mockExecutor, TEST_URI)).thenReturn(JSON_FAKE_LOCKABLE_NOT_LOCKED);

    Lockable response = applicationClient.tryRequest(HttpMethodType.GET, mockExecutor, TEST_URI, FakeLockable.class, 0);

    assertFalse(response.isLockError());
    verify(mockHttpHelper).executeGet(mockExecutor, TEST_URI);
  }

  /**
   * Tests the case where the request works the first time.
   */
  @Test
  public void testRequestWithRetry_OkFirstTry()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(FAKE_APPLICATION)).thenReturn(mockExecutor);
    when(mockHttpHelper.executeGet(mockExecutor, FAKE_APP_URI)).thenReturn(JSON_FAKE_LOCKABLE_NOT_LOCKED);

    Lockable response = applicationClient.requestWithRetry(FAKE_APPLICATION, HttpMethodType.GET, METHOD_PATH, FakeLockable.class);

    assertFalse(response.isLockError());
    verify(mockHttpHelper).executeGet(mockExecutor, FAKE_APP_URI);
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Tests the case where the request fails twice then works the third time.
   */
  @Test
  public void testRequestWithRetry_OkThirdTry() throws InterruptedException
  {
    assertTrue("Test requirement", 3 >= ApplicationClient.MAX_NUM_TRIES);

    Executor mockExecutor = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(FAKE_APPLICATION)).thenReturn(mockExecutor);
    when(mockHttpHelper.executeGet(mockExecutor, FAKE_APP_URI))
        .thenReturn(null)
        .thenReturn(JSON_FAKE_LOCKABLE_LOCKED)
        .thenReturn(JSON_FAKE_LOCKABLE_NOT_LOCKED);

    Lockable response = applicationClient.requestWithRetry(FAKE_APPLICATION, HttpMethodType.GET, METHOD_PATH, FakeLockable.class);

    assertFalse(response.isLockError());
    verify(mockHttpHelper, times(3)).executeGet(mockExecutor, FAKE_APP_URI);
    verify(mockThreadSleeper, times(2)).sleep(anyLong());
  }

  /**
   * Tests the case where the request completely fails (always returns lock error).
   */
  @Test
  public void testRequestWithRetry_AlwaysLocked() throws InterruptedException
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(FAKE_APPLICATION)).thenReturn(mockExecutor);
    when(mockHttpHelper.executePut(mockExecutor, FAKE_APP_URI)).thenReturn(JSON_FAKE_LOCKABLE_LOCKED);

    Lockable response = applicationClient.requestWithRetry(FAKE_APPLICATION, HttpMethodType.PUT, METHOD_PATH, FakeLockable.class);

    assertTrue(response.isLockError());
    verify(mockHttpHelper, times(ApplicationClient.MAX_NUM_TRIES)).executePut(mockExecutor, FAKE_APP_URI);
    verify(mockThreadSleeper, times(ApplicationClient.MAX_NUM_TRIES - 1)).sleep(anyLong());
  }

  /**
   * Tests a successful call to the get-progress resource.
   */
  @Test
  public void testGetDbFreezeProgress()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(FAKE_APPLICATION)).thenReturn(mockExecutor);
    when(mockHttpHelper.executeGet(eq(mockExecutor), anyString())).thenReturn(JSON_DB_FREEZE_PROGRESS);

    DbFreezeProgress response = applicationClient.getDbFreezeProgress(FAKE_APPLICATION);

    assertFalse(response.isLockError());
    assertEquals(DbFreezeMode.NORMAL, response.getMode());
    verify(mockHttpHelper).executeGet(eq(mockExecutor), anyString());
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Tests a successful call to the enter-freeze resource.
   */
  @Test
  public void testPutEnterDbFreeze()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(FAKE_APPLICATION)).thenReturn(mockExecutor);
    when(mockHttpHelper.executePut(eq(mockExecutor), anyString())).thenReturn(JSON_DB_FREEZE_PROGRESS);

    DbFreezeProgress response = applicationClient.putEnterDbFreeze(FAKE_APPLICATION);

    assertFalse(response.isLockError());
    assertEquals(DbFreezeMode.NORMAL, response.getMode());
    verify(mockHttpHelper).executePut(eq(mockExecutor), anyString());
    verifyZeroInteractions(mockThreadSleeper);
  }

  /**
   * Tests a successful call to the enter-freeze resource.
   */
  @Test
  public void testPutDiscoverDb()
  {
    Executor mockExecutor = mock(Executor.class);
    when(mockExecutorFactory.makeAuthenticatedExecutor(FAKE_APPLICATION)).thenReturn(mockExecutor);
    when(mockHttpHelper.executePut(eq(mockExecutor), anyString())).thenReturn(JSON_DISCOVERY_RESULT);

    DiscoveryResult response = applicationClient.putDiscoverDb(FAKE_APPLICATION);

    assertFalse(response.isLockError());
    assertEquals("env1", response.getPhysicalDatabase().getEnvName());
    verify(mockHttpHelper).executePut(eq(mockExecutor), anyString());
    verifyZeroInteractions(mockThreadSleeper);
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