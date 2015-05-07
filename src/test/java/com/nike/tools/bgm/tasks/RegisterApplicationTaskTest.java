package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.TwoEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of RegisterApplicationTask to persist a new Application in the stage env.
 * <p/>
 * Also implicitly tests the ability of TwoEnvTask to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterApplicationTaskTest
{
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @BeforeClass
  public static void stageEnvStartsWithNoApplication()
  {
    FAKE_STAGE_ENV.getApplicationVms().get(0).setApplications(null);
  }

  @InjectMocks
  private RegisterApplicationTask registerApplicationTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private TwoEnvLoader mockTwoEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createTwo(FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName())).thenReturn(mockTwoEnvLoader);
    when(mockTwoEnvLoader.getLiveApplication()).thenReturn(FAKE_LIVE_ENV.getApplicationVms().get(0).getApplications().get(0));
    when(mockTwoEnvLoader.getStageEnv()).thenReturn(FAKE_STAGE_ENV);
    when(mockTwoEnvLoader.getStageApplicationVm()).thenReturn(FAKE_STAGE_ENV.getApplicationVms().get(0));
    registerApplicationTask.assign(1, FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName());
  }

  /**
   * Process with noop should return NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, registerApplicationTask.process(true));
  }

  /**
   * Normal processing should update the env and create the stage application.
   */
  @Test
  public void testProcess_Done()
  {
    assertEquals(TaskStatus.DONE, registerApplicationTask.process(false));
    verify(mockEnvironmentTx).updateEnvironment(FAKE_STAGE_ENV);
    assertEquals(1, FAKE_STAGE_ENV.getApplicationVms().get(0).getApplications().size());
  }
}
