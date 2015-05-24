package bluegreen.manager.model.tx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.EnvironmentTestHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of TwoEnvLoader to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class TwoEnvLoaderTest
{
  private static final String NAME_EMPTY_LIVE = "emptyLive";
  private static final String NAME_EMPTY_STAGE = "emptyStage";
  private static final String NAME_FULL_LIVE = "fullLive";
  private static final String NAME_FULL_STAGE = "fullStage";

  private static final Environment FAKE_EMPTY_LIVE_ENV = EnvironmentTestHelper.makeFakeEnvironment(0);
  private static final Environment FAKE_EMPTY_STAGE_ENV = EnvironmentTestHelper.makeFakeEnvironment(1);
  private static final Environment FAKE_FULL_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_FULL_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  static
  {
    FAKE_EMPTY_LIVE_ENV.setEnvName(NAME_EMPTY_LIVE);
    FAKE_EMPTY_STAGE_ENV.setEnvName(NAME_EMPTY_STAGE);
    FAKE_FULL_LIVE_ENV.setEnvName(NAME_FULL_LIVE);
    FAKE_FULL_STAGE_ENV.setEnvName(NAME_FULL_STAGE);
  }

  @InjectMocks
  private TwoEnvLoader twoEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_EMPTY_LIVE_ENV.getEnvName())).thenReturn(FAKE_EMPTY_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_FULL_LIVE_ENV.getEnvName())).thenReturn(FAKE_FULL_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_EMPTY_STAGE_ENV.getEnvName())).thenReturn(FAKE_EMPTY_STAGE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_FULL_STAGE_ENV.getEnvName())).thenReturn(FAKE_FULL_STAGE_ENV);
  }

  /**
   * Load with both envs fully populated - pass.
   */
  @Test
  public void testLoadDataModel_Pass()
  {
    twoEnvLoader.setLiveEnvName(NAME_FULL_LIVE);
    twoEnvLoader.setStageEnvName(NAME_FULL_STAGE);
    twoEnvLoader.loadDataModel();
    assertEquals(NAME_FULL_LIVE, twoEnvLoader.getLiveEnv().getEnvName());
    assertNotNull(twoEnvLoader.getLiveApplicationVm());
    assertNotNull(twoEnvLoader.getLiveApplication());
    assertNotNull(twoEnvLoader.getLivePhysicalDatabase());
    assertEquals(NAME_FULL_STAGE, twoEnvLoader.getStageEnv().getEnvName());
    assertNotNull(twoEnvLoader.getStageApplicationVm());
    assertNotNull(twoEnvLoader.getStagePhysicalDatabase());
  }

  /**
   * Fail case: missing an application vm.
   */
  @Test(expected = IllegalStateException.class)
  public void testLoadDataModel_NoStageVm()
  {
    twoEnvLoader.setLiveEnvName(NAME_FULL_LIVE);
    twoEnvLoader.setStageEnvName(NAME_EMPTY_STAGE);
    twoEnvLoader.loadDataModel();
  }

  /**
   * Fail case: missing an application.
   */
  @Test(expected = IllegalStateException.class)
  public void testLoadDataModel_NoLiveApplication()
  {
    Environment anotherLiveEnv = EnvironmentTestHelper.makeFakeFullEnvironment(0);
    anotherLiveEnv.getApplicationVms().get(0).setApplications(null);
    final String envName = "NoAppEnv";
    anotherLiveEnv.setEnvName(envName);
    when(mockEnvironmentTx.findNamedEnv(envName)).thenReturn(anotherLiveEnv);
    twoEnvLoader.setLiveEnvName(envName);
    twoEnvLoader.setStageEnvName(NAME_FULL_STAGE);
    twoEnvLoader.loadDataModel();
  }

  /**
   * Fail case: missing a logical database.
   */
  @Test(expected = IllegalStateException.class)
  public void testLoadDataModel_NoLogicalDb()
  {
    Environment anotherStageEnv = EnvironmentTestHelper.makeFakeFullEnvironment(0);
    anotherStageEnv.setLogicalDatabases(null);
    final String envName = "NoLogicalEnv";
    anotherStageEnv.setEnvName(envName);
    when(mockEnvironmentTx.findNamedEnv(envName)).thenReturn(anotherStageEnv);
    twoEnvLoader.setLiveEnvName(NAME_FULL_LIVE);
    twoEnvLoader.setStageEnvName(envName);
    twoEnvLoader.loadDataModel();
  }

}