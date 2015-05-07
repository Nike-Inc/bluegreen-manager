package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.client.app.DiscoveryResult;
import com.nike.tools.bgm.client.app.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of DiscoveryTask to use the applicationClient and check the result.
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoveryTaskTest
{
  private static final Application FAKE_APPLICATION = EnvironmentTestHelper.makeFakeApplication();
  private static final String GOOD_PHYSICALDB_ENV_NAME = FAKE_APPLICATION.getApplicationVm().getEnvironment().getEnvName();
  private static final String GOOD_PHYSICALDB_LOGICAL_NAME = "the-logical-db";
  private static final String GOOD_PHYSICALDB_URL = "jdbc:mysql://db.example.com:3306/dbname";
  private static final String GOOD_PHYSICALDB_USERNAME = "appuser";
  private static final boolean GOOD_PHYSICALDB_IS_LIVE = true;
  private static final PhysicalDatabase GOOD_PHYSICAL_DATABASE = new PhysicalDatabase(
      GOOD_PHYSICALDB_ENV_NAME, GOOD_PHYSICALDB_LOGICAL_NAME, GOOD_PHYSICALDB_URL,
      GOOD_PHYSICALDB_USERNAME, GOOD_PHYSICALDB_IS_LIVE);

  @InjectMocks
  private DiscoveryTask discoveryTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Mock
  protected ApplicationClient mockApplicationClient;

  @Mock
  protected ApplicationSession mockApplicationSession;

  @Before
  public void setUp()
  {
    Environment fakeEnv = FAKE_APPLICATION.getApplicationVm().getEnvironment();
    when(mockEnvLoaderFactory.createOne(fakeEnv.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(fakeEnv);
    when(mockOneEnvLoader.getApplication()).thenReturn(FAKE_APPLICATION);
    when(mockApplicationClient.authenticate(FAKE_APPLICATION)).thenReturn(mockApplicationSession);
    when(mockOneEnvLoader.context()).thenReturn("(Context) ");
    discoveryTask.assign(1, fakeEnv.getEnvName());
  }

  /**
   * Process with noop should return NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, discoveryTask.process(true));
  }

  /**
   * Process obtaining a good discovery result should return DONE.
   */
  @Test
  public void testProcess_Done()
  {
    DiscoveryResult goodResult = new DiscoveryResult(GOOD_PHYSICAL_DATABASE, false, null);
    when(mockApplicationClient.putDiscoverDb(eq(FAKE_APPLICATION), eq(mockApplicationSession), anyInt()))
        .thenReturn(goodResult);

    assertEquals(TaskStatus.DONE, discoveryTask.process(false));

    verify(mockApplicationClient).putDiscoverDb(eq(FAKE_APPLICATION), eq(mockApplicationSession), anyInt());
  }

  @Test(expected = RuntimeException.class)
  public void testCheckResult_NullResult()
  {
    discoveryTask.checkResult(null);
  }

  @Test(expected = RuntimeException.class)
  public void testCheckResult_LockError()
  {
    discoveryTask.checkResult(new DiscoveryResult(GOOD_PHYSICAL_DATABASE, true/*lockError*/, null));
  }

  @Test(expected = RuntimeException.class)
  public void testCheckResult_DiscoveryError()
  {
    discoveryTask.checkResult(new DiscoveryResult(GOOD_PHYSICAL_DATABASE, false, "Discovery ended in a big error"));
  }

  @Test(expected = RuntimeException.class)
  public void testCheckResult_NullPhysicalDatabase()
  {
    discoveryTask.checkResult(new DiscoveryResult(null, false, null));
  }

  @Test(expected = RuntimeException.class)
  public void testCheckResult_WrongEnv()
  {
    PhysicalDatabase badDb = new PhysicalDatabase("the-wrong-env", GOOD_PHYSICALDB_LOGICAL_NAME, GOOD_PHYSICALDB_URL,
        GOOD_PHYSICALDB_USERNAME, GOOD_PHYSICALDB_IS_LIVE);
    discoveryTask.checkResult(new DiscoveryResult(badDb, false, null));
  }

  @Test(expected = RuntimeException.class)
  public void testCheckResult_BadLiveness()
  {
    PhysicalDatabase badDb = new PhysicalDatabase(GOOD_PHYSICALDB_ENV_NAME, GOOD_PHYSICALDB_LOGICAL_NAME, GOOD_PHYSICALDB_URL,
        GOOD_PHYSICALDB_USERNAME, !GOOD_PHYSICALDB_IS_LIVE);
    discoveryTask.checkResult(new DiscoveryResult(badDb, false, null));
  }
}