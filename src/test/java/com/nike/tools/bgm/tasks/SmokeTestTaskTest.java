package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Not much to unit test other than assign.
 */
@RunWith(MockitoJUnitRunner.class)
public class SmokeTestTaskTest
{
  protected static final Application FAKE_APPLICATION = EnvironmentTestHelper.makeFakeApplication();

  @InjectMocks
  private SmokeTestTask smokeTestTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Mock
  private ApplicationClient mockApplicationClient;

  @Mock
  private ApplicationSession mockApplicationSession;

  @Before
  public void setUp()
  {
    Environment fakeEnv = FAKE_APPLICATION.getApplicationVm().getEnvironment();
    when(mockEnvLoaderFactory.createOne(fakeEnv.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(fakeEnv);
    when(mockOneEnvLoader.getApplication()).thenReturn(FAKE_APPLICATION);
    when(mockApplicationClient.authenticate(FAKE_APPLICATION)).thenReturn(mockApplicationSession);
    when(mockOneEnvLoader.context()).thenReturn("(Context) ");
    smokeTestTask.assign(1, fakeEnv.getEnvName());
  }

  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, smokeTestTask.process(true));
    verify(mockOneEnvLoader).loadApplication();
    verifyZeroInteractions(mockApplicationClient);
  }

  @Test
  public void testProcess_Done()
  {
    assertEquals(TaskStatus.DONE, smokeTestTask.process(false));
    verify(mockOneEnvLoader).loadApplication();
    verify(mockApplicationClient).authenticate(FAKE_APPLICATION);
  }
}