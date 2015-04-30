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
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @InjectMocks
  private LinkLiveDatabaseTask linkLiveDatabaseTask;

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_LIVE_ENV.getEnvName())).thenReturn(FAKE_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_STAGE_ENV.getEnvName())).thenReturn(FAKE_STAGE_ENV);
    linkLiveDatabaseTask.assign(1, FAKE_LIVE_ENV.getEnvName() /*old*/, FAKE_STAGE_ENV.getEnvName() /*new*/);
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
    assertNotEquals(getPhysicalDatabaseFromEnvironment(FAKE_LIVE_ENV).getInstanceName(),
        getPhysicalDatabaseFromEnvironment(FAKE_STAGE_ENV).getInstanceName());

    assertEquals(TaskStatus.DONE, linkLiveDatabaseTask.process(false));

    verify(mockEnvironmentTx).updateEnvironment(FAKE_STAGE_ENV);
    assertEquals(getPhysicalDatabaseFromEnvironment(FAKE_LIVE_ENV).getInstanceName(),
        getPhysicalDatabaseFromEnvironment(FAKE_STAGE_ENV).getInstanceName());
  }

  /**
   * Assumes exactly 1 logicaldb.
   */
  private PhysicalDatabase getPhysicalDatabaseFromEnvironment(Environment environment)
  {
    return environment.getLogicalDatabases().get(0).getPhysicalDatabase();
  }
}