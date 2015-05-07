package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.TwoEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of LinkLiveDatabaseTask to update stage physicaldb to be same as live physicaldb.
 * <p/>
 * Also implicitly tests the ability of TwoEnvTask to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class LinkLiveDatabaseTaskTest
{
  @InjectMocks
  private LinkLiveDatabaseTask linkLiveDatabaseTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private TwoEnvLoader mockTwoEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  /*
  Note: linkLiveDatabaseTask.process(false) modifies the contents of the env objects.
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
    linkLiveDatabaseTask.assign(1, fakeLiveEnv.getEnvName() /*old*/, fakeStageEnv.getEnvName() /*new*/);
  }

  /**
   * Process with noop should return NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, linkLiveDatabaseTask.process(true));
  }

  /**
   * Normal processing should update the stage env with a physicaldb same as live env.
   */
  @Test
  public void testProcess_Done()
  {
    assertNotEquals(getPhysicalDatabaseFromEnvironment(fakeLiveEnv).getInstanceName(),
        getPhysicalDatabaseFromEnvironment(fakeStageEnv).getInstanceName());

    assertEquals(TaskStatus.DONE, linkLiveDatabaseTask.process(false));

    verify(mockEnvironmentTx).updateEnvironment(fakeStageEnv);
    assertEquals(getPhysicalDatabaseFromEnvironment(fakeLiveEnv).getInstanceName(),
        getPhysicalDatabaseFromEnvironment(fakeStageEnv).getInstanceName());
  }

  /**
   * Assumes exactly 1 logicaldb.
   */
  private PhysicalDatabase getPhysicalDatabaseFromEnvironment(Environment environment)
  {
    return environment.getLogicalDatabases().get(0).getPhysicalDatabase();
  }
}