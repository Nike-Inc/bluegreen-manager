package com.nike.tools.bgm.model.tx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of OneEnvLoader to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneEnvLoaderTest
{
  private static final Environment FAKE_EMPTY_ENV = EnvironmentTestHelper.makeFakeEnvironment(0);
  private static final Environment FAKE_FULL_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @InjectMocks
  private OneEnvLoader oneEnvLoader;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_EMPTY_ENV.getEnvName())).thenReturn(FAKE_EMPTY_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_FULL_ENV.getEnvName())).thenReturn(FAKE_FULL_ENV);
  }

  /**
   * Create a vm: should not already exist.
   */
  @Test
  public void testLoadApplicationVmCreate_Pass()
  {
    oneEnvLoader.setEnvName(FAKE_EMPTY_ENV.getEnvName());
    oneEnvLoader.loadApplicationVm(true);
    assertEquals(FAKE_EMPTY_ENV, oneEnvLoader.getEnvironment());
    assertNull(oneEnvLoader.getApplicationVm());
    assertNull(oneEnvLoader.getApplication());
  }

  /**
   * Fail: want to create vm but it already exists.
   */
  @Test(expected = IllegalStateException.class)
  public void testLoadApplicationVmCreate_VmExists()
  {
    oneEnvLoader.setEnvName(FAKE_FULL_ENV.getEnvName());
    oneEnvLoader.loadApplicationVm(true);
  }

  /**
   * Modify a vm: should already exist.
   */
  @Test
  public void testLoadApplicationVmModify_Pass()
  {
    oneEnvLoader.setEnvName(FAKE_FULL_ENV.getEnvName());
    oneEnvLoader.loadApplicationVm(false);
    assertEquals(FAKE_FULL_ENV, oneEnvLoader.getEnvironment());
    assertNotNull(oneEnvLoader.getApplicationVm());
  }

  /**
   * Fail: want to modify a vm but none exist.
   */
  @Test(expected = IllegalStateException.class)
  public void testLoadApplicationVmModify_NoVms()
  {
    oneEnvLoader.setEnvName(FAKE_EMPTY_ENV.getEnvName());
    oneEnvLoader.loadApplicationVm(false);
  }

  /**
   * Pass: load application in a fullly populated environment.
   */
  @Test
  public void testLoadApplication_Pass()
  {
    oneEnvLoader.setEnvName(FAKE_FULL_ENV.getEnvName());
    oneEnvLoader.loadApplication();
    assertEquals(FAKE_FULL_ENV, oneEnvLoader.getEnvironment());
    assertNotNull(oneEnvLoader.getApplicationVm());
  }

  /**
   * Fail: env and vm exist but application does not.
   */
  @Test(expected = IllegalStateException.class)
  public void testLoadApplication_NoApplication()
  {
    Environment anotherFullEnv = EnvironmentTestHelper.makeFakeFullEnvironment(1);
    anotherFullEnv.getApplicationVms().get(0).setApplications(null);
    when(mockEnvironmentTx.findNamedEnv(anotherFullEnv.getEnvName())).thenReturn(anotherFullEnv);
    oneEnvLoader.setEnvName(anotherFullEnv.getEnvName());
    oneEnvLoader.loadApplication();
  }

  /**
   * Empty environment: context is only the environment.
   */
  @Test
  public void testContext_EmptyEnvironment()
  {
    oneEnvLoader.setEnvName(FAKE_EMPTY_ENV.getEnvName());
    oneEnvLoader.loadApplicationVm(true);
    String context = oneEnvLoader.context();
    assertTrue(context.contains("environment"));
    assertFalse(context.contains(","));
  }

  /**
   * Full environment: context is the env, the vm and application.
   */
  @Test
  public void testContext_FullEnvironment()
  {
    oneEnvLoader.setEnvName(FAKE_FULL_ENV.getEnvName());
    oneEnvLoader.loadApplication();
    String context = oneEnvLoader.context();
    assertTrue(context.contains("environment"));
    assertTrue(context.contains(","));
  }

}