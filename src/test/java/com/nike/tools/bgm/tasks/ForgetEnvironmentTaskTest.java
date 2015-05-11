package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability to drop an environment entity.
 */
@RunWith(MockitoJUnitRunner.class)
public class ForgetEnvironmentTaskTest
{
  private static final Environment FAKE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);

  @InjectMocks
  private ForgetEnvironmentTask forgetEnvironmentTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(FAKE_ENV.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(FAKE_ENV);
    forgetEnvironmentTask.assign(1, FAKE_ENV.getEnvName());
  }

  /**
   * Process with noop should return NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, forgetEnvironmentTask.process(true));
  }

  /**
   * Normal processing should delete the environment.
   */
  @Test
  public void testProcess_Done()
  {
    assertEquals(TaskStatus.DONE, forgetEnvironmentTask.process(false));
    verify(mockEnvironmentTx).deleteEnvironment(FAKE_ENV);
  }

}