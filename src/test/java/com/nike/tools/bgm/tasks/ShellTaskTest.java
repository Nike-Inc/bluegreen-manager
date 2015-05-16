package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.substituter.OneEnvStringSubstituter;
import com.nike.tools.bgm.substituter.StringSubstituterFactory;
import com.nike.tools.bgm.substituter.TwoEnvStringSubstituter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the common features of a ShellTask.
 */
@RunWith(MockitoJUnitRunner.class)
public class ShellTaskTest
{
  private static final String ENV1 = "env1";
  private static final String ENV2 = "env2";
  private static final String COMMAND = "runStuff --arg %{arg}";
  private static final String REGEXP_ERROR = "(FATAL|There was an ERROR)";
  private static final String GOOD_OUTPUT = "This output\nIs just fine!";
  private static final String BAD_OUTPUT = "This line is ok but now...\nFATAL: something bad happened";
  private static final Integer EXITCODE_SUCCESS = 0;
  private static final Integer EXITCODE_ERROR = 1;

  @InjectMocks
  private ShellTask shellTask = new ShellTask()
  {
    @Override
    public TaskStatus process(boolean noop)
    {
      throw new UnsupportedOperationException(); //Should not be invoked during these tests
    }
  };

  @Mock
  private StringSubstituterFactory mockStringSubstituterFactory;

  @Mock
  private TwoEnvStringSubstituter mockTwoEnvStringSubstituter;

  @Mock
  private OneEnvStringSubstituter mockOneEnvStringSubstituter;

  @Before
  public void setUp()
  {
    when(mockStringSubstituterFactory.createTwo(anyString(), anyString(), anyMap())).thenReturn(mockTwoEnvStringSubstituter);
    when(mockStringSubstituterFactory.createOne(anyString(), anyMap())).thenReturn(mockOneEnvStringSubstituter);

  }

  private void setupTwoEnvProperly()
  {
    shellTask.assign(1, ENV1, ENV2, new ShellConfig(COMMAND, REGEXP_ERROR, EXITCODE_SUCCESS, null));
  }

  private void setupTwoEnv()
  {
    shellTask.assign(1, ENV1, ENV2, new ShellConfig());
  }

  private void setupOneEnv()
  {
    shellTask.assign(1, ENV1, new ShellConfig());
  }

  @Test
  public void testAssignTwo()
  {
    setupTwoEnv();
    verify(mockStringSubstituterFactory).createTwo(ENV1, ENV2, null);
  }

  @Test
  public void testAssignOne()
  {
    setupOneEnv();
    verify(mockStringSubstituterFactory).createOne(ENV1, null);
  }

  @Test
  public void testLoadDataModel_Two()
  {
    setupTwoEnv();
    shellTask.loadDataModel();
    verify(mockTwoEnvStringSubstituter).loadDataModel();
  }

  @Test
  public void testLoadDataModel_One()
  {
    setupOneEnv();
    shellTask.loadDataModel();
    verify(mockOneEnvStringSubstituter).loadDataModel();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckConfig_Fail()
  {
    setupTwoEnv(); //Empty shellConfig
    shellTask.checkConfig();
  }

  @Test
  public void testCheckConfig_Pass()
  {
    setupTwoEnvProperly();
    shellTask.checkConfig();
  }

  @Test
  public void testCheckOutput_Match()
  {
    setupTwoEnvProperly();
    assertTrue(shellTask.checkOutput(GOOD_OUTPUT));
    assertFalse(shellTask.checkOutput(BAD_OUTPUT));
  }

  /**
   * Errors go unnoticed if no error-regexp was specified.
   */
  @Test
  public void testCheckOutput_NoRegexp()
  {
    setupTwoEnv();
    assertTrue(shellTask.checkOutput(GOOD_OUTPUT));
    assertTrue(shellTask.checkOutput(BAD_OUTPUT));
  }

  @Test
  public void testCheckExitValue_Match()
  {
    setupTwoEnvProperly();
    assertTrue(shellTask.checkExitValue(EXITCODE_SUCCESS));
    assertFalse(shellTask.checkExitValue(EXITCODE_ERROR));
  }

  /**
   * Errors go unnoticed if no success-value was specified.
   */
  @Test
  public void testCheckExitValue_NoValue()
  {
    setupTwoEnv();
    assertTrue(shellTask.checkExitValue(EXITCODE_SUCCESS));
    assertTrue(shellTask.checkExitValue(EXITCODE_ERROR));
  }

  @Test
  public void testCheckForErrors()
  {
    setupTwoEnvProperly();
    assertEquals(TaskStatus.DONE, shellTask.checkForErrors(GOOD_OUTPUT, EXITCODE_SUCCESS));
    assertEquals(TaskStatus.ERROR, shellTask.checkForErrors(GOOD_OUTPUT, EXITCODE_ERROR));
  }

  /**
   * Nothing to assert, except that it doesn't throw.
   */
  @Test
  public void testLogExitValue()
  {
    setupTwoEnv();
    shellTask.logExitValue(EXITCODE_SUCCESS);
  }
}