package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.main.ArgumentParser;
import com.nike.tools.bgm.main.CmdlineException;

import static com.nike.tools.bgm.jobs.JobFactory.UNLIMITED_NUM_VALUES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that a JobFactory can create jobs based on cmdline arguments.
 */
@RunWith(MockitoJUnitRunner.class)
public class JobFactoryTest
{
  private static final List<String> PARAM_FOO = Arrays.asList("foo", "f1");
  private static final List<String> PARAM_BAR = Arrays.asList("bar", "b1", "b2");
  private static final List<String> PARAM_BAZ = Arrays.asList("baz");

  @InjectMocks
  private JobFactory jobFactory;

  @Mock
  private ApplicationContext mockApplicationContext;

  @Mock
  private JobHistoryTx mockJobHistoryTx;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  private ArgumentParser argumentParser = new ArgumentParser();

  /**
   * Tests that the argument explanation describes the main job types.
   */
  @Test
  public void testMakeExplanationOfValidJobs()
  {
    String explanation = jobFactory.makeExplanationOfValidJobs();
    assertTrue(explanation.contains(JobFactory.JOBNAME_STAGING_DEPLOY));
    assertTrue(explanation.contains(JobFactory.JOBNAME_GO_LIVE));
    assertTrue(explanation.contains(JobFactory.JOBNAME_TEARDOWN));
  }

  /**
   * Helper - calls the argument parser, then the method under test.
   */
  private Job parseAndMakeJob(String commandLine)
  {
    argumentParser.parseArgs(commandLine.split("\\s+"));
    return jobFactory.makeJob(argumentParser.getJobName(), argumentParser.getParameters(), argumentParser.getCommandLine());
  }

  /**
   * Fail case: invalid job name.
   */
  @Test(expected = CmdlineException.class)
  public void testMakeJob_BadJobName()
  {
    parseAndMakeJob("nonexistentJob --someArg");
  }

  /**
   * Tests successful creation of a StagingDeployJob.
   */
  @Test
  public void testMakeJob_StagingDeploy()
  {
    when(mockEnvironmentTx.checkIfEnvNamesExist(anyString())).thenReturn(new boolean[] { true });
    String commandLine = "stagingDeploy --liveEnv env1 --stageEnv env2 --noop --packages a b c --dbMap d e";
    parseAndMakeJob(commandLine);
    verify(mockApplicationContext).getBean(eq(StagingDeployJob.class), new Object[] {
        eq(commandLine), eq(true)/*noop*/, eq(false), isNull(), eq("env1"), eq("env2"), anyMap(), anyListOf(String.class)
    });
  }

  /**
   * Tests successful creation of a GoLiveJob.
   */
  @Test
  public void testMakeJob_GoLive()
  {
    when(mockEnvironmentTx.checkIfEnvNamesExist(anyString(), anyString())).thenReturn(new boolean[] { true, true });
    String commandLine = "goLive --oldLiveEnv env3 --newLiveEnv env4 --force --fixedLB lb1";
    parseAndMakeJob(commandLine);
    verify(mockApplicationContext).getBean(eq(GoLiveJob.class), new Object[] {
        eq(commandLine), eq(false), eq(true)/*force*/, isNull(), eq("env3"), eq("env4"), eq("lb1")
    });
  }

  /**
   * Tests successful creation of a TeardownJob.
   */
  @Test
  public void testMakeJob_Teardown()
  {
    when(mockEnvironmentTx.checkIfEnvNamesExist(anyString())).thenReturn(new boolean[] { true });
    String commandLine = "teardown --deleteEnv env5 --deleteDb db1 --stopServices a,b --commit";
    parseAndMakeJob(commandLine);
    verify(mockApplicationContext).getBean(eq(TeardownJob.class), new Object[] {
        eq(commandLine), eq(false), eq(false), isNull(), eq("env5"), eq("db1"), anyListOf(String.class), eq(true)
    });
  }

  /**
   * Fail case: desired parameter not found in the list.
   */
  @Test(expected = CmdlineException.class)
  public void testGetParameter_NotFound()
  {
    jobFactory.getParameter("a", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ), UNLIMITED_NUM_VALUES);
  }

  /**
   * Fail case: desired parameter found but wrong number of args.
   */
  @Test(expected = CmdlineException.class)
  public void testGetParameter_WrongNumArgs()
  {
    jobFactory.getParameter("bar", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ), 1);
  }

  /**
   * Pass case: desired parameter found, with expected number of args.
   */
  @Test
  public void testGetParameter_Pass()
  {
    List<String> param = jobFactory.getParameter("bar", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ), 2);
    assertEquals("bar", param.get(0));
    assertEquals(3, param.size());
  }

  /**
   * Case where desired param is found but it has no values so throws.
   */
  @Test(expected = CmdlineException.class)
  public void testGetParameterValues_NoValues()
  {
    jobFactory.getParameterValues("baz", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ));
  }

  /**
   * Case where desired param is found and it has some values.
   */
  @Test
  public void testGetParameterValues_Found()
  {
    List<String> param = jobFactory.getParameterValues("bar", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ));
    assertEquals("b1", param.get(0));
    assertEquals(2, param.size());
  }

  /**
   * Tests that a paramName is not in an empty list of parameter sublists.
   */
  @Test
  public void testHasParameter_NoParams()
  {
    assertFalse(jobFactory.hasParameter("a", new ArrayList<List<String>>()));
  }

  /**
   * Tests that a paramName is not in a normal list of parameters.
   */
  @Test
  public void testHasParameter_No()
  {
    assertFalse(jobFactory.hasParameter("a", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ)));
  }

  /**
   * Tests that a paramName is in a list of parameters.
   */
  @Test
  public void testHasParameter_Yes()
  {
    assertTrue(jobFactory.hasParameter("bar", Arrays.asList(PARAM_FOO, PARAM_BAR, PARAM_BAZ)));
  }

  /**
   * Fail case: no args.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testVerifyEnvNames_BadArgs()
  {
    jobFactory.verifyEnvNames(new String[] { });
  }

  /**
   * Fail case: environmentTx returns a result array whose size doesn't match the number of envNames.
   */
  @Test(expected = RuntimeException.class)
  public void testVerifyEnvNames_ArraySizeMismatch()
  {
    String[] envNames = new String[] { "env1", "env2" };
    boolean[] exists = new boolean[] { true };
    when(mockEnvironmentTx.checkIfEnvNamesExist(envNames)).thenReturn(exists);
    jobFactory.verifyEnvNames(envNames);
  }

  /**
   * Fail case: environmentTx reports that one envName is invalid.
   */
  @Test(expected = CmdlineException.class)
  public void testVerifyEnvNames_OneInvalid()
  {
    String[] envNames = new String[] { "env1", "env2" };
    boolean[] exists = new boolean[] { true, false };
    when(mockEnvironmentTx.checkIfEnvNamesExist(envNames)).thenReturn(exists);
    jobFactory.verifyEnvNames(envNames);
  }

  /**
   * Pass case: environmentTx reports that all envNames are valid.
   */
  @Test
  public void testVerifyEnvNames_Pass()
  {
    String[] envNames = new String[] { "env1", "env2" };
    boolean[] exists = new boolean[] { true, true };
    when(mockEnvironmentTx.checkIfEnvNamesExist(envNames)).thenReturn(exists);
    jobFactory.verifyEnvNames(envNames);
    verify(mockEnvironmentTx).checkIfEnvNamesExist(envNames);
  }

  /**
   * Fail case: odd number of args can't be made into a map.
   */
  @Test(expected = CmdlineException.class)
  public void testListToMap_OddArgs()
  {
    jobFactory.listToMap(Arrays.asList("a", "b", "c"), "someParam");
  }

  /**
   * Fail case: zero args can't be made into a map.
   */
  @Test(expected = CmdlineException.class)
  public void testListToMap_NoArgs()
  {
    jobFactory.listToMap(new ArrayList<String>(), "someParam");
  }

  /**
   * Pass case: even number of args turns into a map.
   */
  @Test
  public void testListToMap_Pass()
  {
    Map<String, String> map = jobFactory.listToMap(Arrays.asList("a", "b", "c", "d"), "someParam");
    assertEquals(2, map.size());
    assertEquals("b", map.get("a"));
    assertEquals("d", map.get("c"));
  }
}
