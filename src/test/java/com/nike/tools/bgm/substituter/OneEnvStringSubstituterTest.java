package com.nike.tools.bgm.substituter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OneEnvStringSubstituterTest
{
  private static final Environment FAKE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Map<String, String> EXTRA = new HashMap<String, String>()
  {{
      put("%{extra}", "One Extra Substitution Value");
    }};

  @InjectMocks
  private OneEnvStringSubstituter oneEnvStringSubstituter = new OneEnvStringSubstituter(FAKE_ENV.getEnvName(), EXTRA);

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Before
  public void setUp()
  {
    when(mockOneEnvLoader.getApplicationVm()).thenReturn(FAKE_ENV.getApplicationVms().get(0));
  }

  /**
   * Substitution should support the OneEnv %{..} variables: vmHostname.
   * And also support Extra.
   */
  @Test
  public void testSubstituteVariables()
  {
    String command = "run stuff with VM_HOSTNAME=%{vmHostname}; EXTRA=%{extra}!";
    String result = oneEnvStringSubstituter.substituteVariables(command);
    assertTrue(result.contains("VM_HOSTNAME=" + FAKE_ENV.getApplicationVms().get(0).getHostname()));
    assertTrue(result.contains("EXTRA=One Extra Substitution Value"));
  }
}