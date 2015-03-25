package com.nike.tools.bgm.client.http;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationTestHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExecutorFactoryTest
{
  private static final String USERNAME = "theUser";
  private static final String PASSWORD = "thePassword";

  @InjectMocks
  private ExecutorFactory executorFactory;

  @Mock
  private HttpClient mockHttpClient;

  @Mock
  private ExecutorFactoryFactory mockExecutorFactoryFactory;

  @Before
  public void setUp()
  {
    executorFactory.setApplicationUsername(USERNAME);
    executorFactory.setApplicationPassword(PASSWORD);
  }

  /**
   * Tests the ability to make an authenticated http executor.
   * <p/>
   * Not really verifying much, such we're mocking the executor.
   */
  @Test
  public void testMakeAuthenticatedExecutor() throws Exception
  {
    Application application = ApplicationTestHelper.makeFakeApplication();
    Executor httpExecutor = mock(Executor.class);
    when(mockExecutorFactoryFactory.newInstance(mockHttpClient)).thenReturn(httpExecutor);
    when(httpExecutor.auth(any(HttpHost.class), anyString(), anyString())).thenReturn(httpExecutor);

    executorFactory.makeAuthenticatedExecutor(application);

    verify(httpExecutor).auth(any(HttpHost.class), eq(USERNAME), eq(PASSWORD));
  }

}