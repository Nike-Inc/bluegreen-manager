package bluegreen.manager.substituter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.EnvironmentTestHelper;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.OneEnvLoader;
import static bluegreen.manager.substituter.StringSubstituter.BLEEP;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OneEnvStringSubstituterTest
{
  private static final Environment FAKE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Map<String, String> EXTRA = new HashMap<String, String>()
  {{
      put("extra", "One Extra Substitution Value");
    }};

  @InjectMocks
  private OneEnvStringSubstituter oneEnvStringSubstituter = new OneEnvStringSubstituter(FAKE_ENV.getEnvName(), EXTRA);

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(FAKE_ENV.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getApplicationVm()).thenReturn(FAKE_ENV.getApplicationVms().get(0));
  }

  @Test
  public void testLoadDataModel()
  {
    oneEnvStringSubstituter.loadDataModel();
    verify(mockOneEnvLoader).loadApplication();
  }

  /**
   * Substitution should support the OneEnv %{..} variables: vmHostname.
   * And also support Extra.
   */
  @Test
  public void testSubstituteVariables()
  {
    oneEnvStringSubstituter.loadDataModel();
    String template = "run stuff with VM_HOSTNAME=%{vmHostname}; EXTRA=%{{extra}}!";
    SubstituterResult result = oneEnvStringSubstituter.substituteVariables(template);
    assertTrue(result.getSubstituted().contains("VM_HOSTNAME=" + FAKE_ENV.getApplicationVms().get(0).getHostname()));
    assertTrue(result.getSubstituted().contains("EXTRA=One Extra Substitution Value"));
    assertTrue(result.getExpurgated().contains("EXTRA=" + BLEEP));
  }
}