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
import com.nike.tools.bgm.model.tx.TwoEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability to swap the physicaldb links of the live and stage envs.
 */
@RunWith(MockitoJUnitRunner.class)
public class SwapDatabasesTaskTest
{
  @InjectMocks
  private SwapDatabasesTask swapDatabasesTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private TwoEnvLoader mockTwoEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  /*
  Note: task processing modifies the contents of the env objects.
   */
  private final Environment fakeLiveEnv = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private final Environment fakeStageEnv = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createTwo(fakeLiveEnv.getEnvName(), fakeStageEnv.getEnvName())).thenReturn(mockTwoEnvLoader);
    when(mockTwoEnvLoader.getLivePhysicalDatabase()).thenReturn(fakeLiveEnv.getLogicalDatabases().get(0).getPhysicalDatabase());
    when(mockTwoEnvLoader.getStageEnv()).thenReturn(fakeStageEnv);
    when(mockTwoEnvLoader.getStagePhysicalDatabase()).thenReturn(fakeStageEnv.getLogicalDatabases().get(0).getPhysicalDatabase());
    swapDatabasesTask.assign(1, fakeLiveEnv.getEnvName() /*old*/, fakeStageEnv.getEnvName() /*new*/);
  }

  /**
   * Process with noop should return NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, swapDatabasesTask.process(true));
  }

  /**
   * Normal processing should update the stage env with a physicaldb same as live env.
   */
  @Test
  public void testProcess_Done()
  {
    String beforeLiveEnvDatabase = getPhysicalDatabaseInstanceName(fakeLiveEnv);
    String beforeStageEnvDatabase = getPhysicalDatabaseInstanceName(fakeStageEnv);
    assertNotEquals(beforeLiveEnvDatabase, beforeStageEnvDatabase);

    assertEquals(TaskStatus.DONE, swapDatabasesTask.process(false));

    verify(mockEnvironmentTx).updateEnvironment(fakeLiveEnv);
    verify(mockEnvironmentTx).updateEnvironment(fakeStageEnv);
    String afterLiveEnvDatabase = getPhysicalDatabaseInstanceName(fakeLiveEnv);
    String afterStageEnvDatabase = getPhysicalDatabaseInstanceName(fakeStageEnv);
    assertEquals(afterLiveEnvDatabase, beforeStageEnvDatabase);
    assertEquals(afterStageEnvDatabase, beforeLiveEnvDatabase);
  }

  /**
   * Returns the instance name of the physicaldb in this env.  Assumes exactly 1 logicaldb.
   */
  private String getPhysicalDatabaseInstanceName(Environment environment)
  {
    return environment.getLogicalDatabases().get(0).getPhysicalDatabase().getInstanceName();
  }
}