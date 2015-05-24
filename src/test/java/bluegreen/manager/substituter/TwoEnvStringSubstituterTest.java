package bluegreen.manager.substituter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.EnvironmentTestHelper;
import bluegreen.manager.model.domain.PhysicalDatabase;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.TwoEnvLoader;
import static bluegreen.manager.substituter.StringSubstituter.BLEEP;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TwoEnvStringSubstituterTest
{
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);
  private static final Map<String, String> EXTRA = new HashMap<String, String>()
  {{
      put("extra", "Extra Substitution Value");
    }};

  @InjectMocks
  private TwoEnvStringSubstituter twoEnvStringSubstituter = new TwoEnvStringSubstituter(FAKE_LIVE_ENV.getEnvName(),
      FAKE_STAGE_ENV.getEnvName(), EXTRA);

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private TwoEnvLoader mockTwoEnvLoader;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createTwo(FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName())).thenReturn(mockTwoEnvLoader);
    when(mockTwoEnvLoader.getLiveApplicationVm()).thenReturn(FAKE_LIVE_ENV.getApplicationVms().get(0));
    when(mockTwoEnvLoader.getStageApplicationVm()).thenReturn(FAKE_STAGE_ENV.getApplicationVms().get(0));
    when(mockTwoEnvLoader.getLivePhysicalDatabase()).thenReturn(FAKE_LIVE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase());
    when(mockTwoEnvLoader.getStagePhysicalDatabase()).thenReturn(FAKE_STAGE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase());
  }

  @Test
  public void testLoadDataModel()
  {
    twoEnvStringSubstituter.loadDataModel();
    verify(mockTwoEnvLoader).loadDataModel();
  }

  /**
   * Substitution should support these TwoEnv %{..} variables: liveEnv, stageEnv, applicationVmMap, physicalDbMap.
   * And also support Extra.
   */
  @Test
  public void testSubstituteVariables()
  {
    twoEnvStringSubstituter.loadDataModel();
    String template = "run stuff with LIVE_ENV=%{liveEnv}; STAGE_ENV=%{{stageEnv}}; APPLICATION_VM_MAP=%{applicationVmMap}; PHYSICAL_DB_MAP=%{physicalDbMap}; EXTRA=%{extra}!";
    SubstituterResult result = twoEnvStringSubstituter.substituteVariables(template);
    assertTrue(result.getSubstituted().contains("LIVE_ENV=" + FAKE_LIVE_ENV.getEnvName()));
    assertTrue(result.getSubstituted().contains("STAGE_ENV=" + FAKE_STAGE_ENV.getEnvName()));
    assertTrue(result.getExpurgated().contains("STAGE_ENV=" + BLEEP));
    ApplicationVm liveApplicationVm = FAKE_LIVE_ENV.getApplicationVms().get(0);
    ApplicationVm stageApplicationVm = FAKE_STAGE_ENV.getApplicationVms().get(0);
    assertTrue(Pattern.compile("APPLICATION_VM_MAP=" + liveApplicationVm.getHostname() + ".*" + stageApplicationVm.getHostname() + ".*;")
        .matcher(result.getSubstituted()).find());
    PhysicalDatabase livePhysicalDatabase = FAKE_LIVE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase();
    PhysicalDatabase stagePhysicalDatabase = FAKE_STAGE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase();
    assertTrue(Pattern.compile("PHYSICAL_DB_MAP=" + livePhysicalDatabase.getInstanceName() + ".*" + stagePhysicalDatabase.getInstanceName() + ".*;")
        .matcher(result.getSubstituted()).find());
    assertTrue(result.getSubstituted().contains("EXTRA=Extra Substitution Value"));
  }

}