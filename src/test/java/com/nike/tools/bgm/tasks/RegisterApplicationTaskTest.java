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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of TwoEnvTask to get environmental info.
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
  protected EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_LIVE_ENV.getEnvName())).thenReturn(FAKE_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_STAGE_ENV.getEnvName())).thenReturn(FAKE_STAGE_ENV);
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
