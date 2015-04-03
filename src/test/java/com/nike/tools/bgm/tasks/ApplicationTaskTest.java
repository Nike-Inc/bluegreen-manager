package com.nike.tools.bgm.tasks;

import org.apache.http.Header;
import org.apache.http.client.fluent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of ApplicationTask to get environmental info and use the applicationClient.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationTaskTest
{
  protected static final Application FAKE_APPLICATION = ApplicationTestHelper.makeFakeApplication();

  @InjectMocks
  private ApplicationTask applicationTask = new ApplicationTask()
  {
    @Override
    public TaskStatus process(boolean noop)
    {
      throw new UnsupportedOperationException();//won't be tested here
    }
  };

  @Mock
  protected ApplicationClient mockApplicationClient;

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Mock
  protected Executor mockExecutor;

  @Mock
  protected Header mockCookieHeader;

  @Before
  public void setUp()
  {
    ApplicationSession fakeSession = new ApplicationSession(mockExecutor, mockCookieHeader);
    String envName = FAKE_APPLICATION.getApplicationVm().getEnvironment().getEnvName();
    when(mockEnvironmentTx.findNamedEnv(envName)).thenReturn(FAKE_APPLICATION.getApplicationVm().getEnvironment());
    applicationTask.init(1, envName);
  }

  /**
   * Tests that we can make a nice env/vm/app context string for logging.
   */
  @Test
  public void testContext()
  {
    String str = applicationTask.context();
    assertTrue(str.contains("environment"));
    assertTrue(str.contains("http"));
  }

}