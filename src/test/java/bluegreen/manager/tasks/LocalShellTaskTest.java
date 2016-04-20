package bluegreen.manager.tasks;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.EnvironmentTestHelper;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.substituter.OneEnvStringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;
import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.substituter.TwoEnvStringSubstituter;
import bluegreen.manager.utils.ProcessBuilderAdapter;
import bluegreen.manager.utils.ProcessBuilderAdapterFactory;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LocalShellTaskTest
{
  private static final String COMMAND = "run some stuff";
  private static final SubstituterResult SUBSTITUTED_COMMAND = new SubstituterResult(COMMAND, COMMAND);
  private static final String REGEXP_ERROR = "(An ERROR happened!|Bad stuff)";
  private static final Integer EXITCODE_SUCCESS = 0;
  private static final Integer EXITCODE_ERROR = 1;
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @InjectMocks
  private LocalShellTask localShellTask;

  @Mock
  private StringSubstituterFactory mockStringSubstituterFactory;

  @Mock
  private OneEnvStringSubstituter mockOneEnvStringSubstituter;

  @Mock
  private TwoEnvStringSubstituter mockTwoEnvStringSubstituter;

  @Mock
  private ProcessBuilderAdapterFactory mockProcessBuilderAdapterFactory;

  @Mock
  private ProcessBuilderAdapter mockProcessBuilderAdapter;

  @Mock
  private Process mockProcess;

  private ShellConfig shellConfig = new ShellConfig(COMMAND, null, REGEXP_ERROR, EXITCODE_SUCCESS, null);

  @Before
  public void setUpTwoEnv()
  {
    when(mockStringSubstituterFactory.createTwo(anyString(), anyString(), anyMapOf(String.class, String.class)))
        .thenReturn(mockTwoEnvStringSubstituter);
    when(mockTwoEnvStringSubstituter.substituteVariables(anyString())).thenReturn(SUBSTITUTED_COMMAND);
    localShellTask.assign(1, FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName(), shellConfig, true);
  }

  private void setUpProcessBuilder(String fakeOutput, int fakeExitValue) throws IOException
  {
    when(mockProcessBuilderAdapterFactory.create(any(String[].class))).thenReturn(mockProcessBuilderAdapter);
    when(mockProcessBuilderAdapter.redirectErrorStream(anyBoolean())).thenReturn(mockProcessBuilderAdapter);
    when(mockProcessBuilderAdapter.start()).thenReturn(mockProcess);
    when(mockProcess.getInputStream()).thenReturn(IOUtils.toInputStream(fakeOutput));
    when(mockProcess.exitValue()).thenReturn(fakeExitValue);
  }

  private void verifyProcessBuilder() throws IOException
  {
    verify(mockProcessBuilderAdapterFactory).create(any(String[].class));
    verify(mockProcessBuilderAdapter).start();
    verify(mockProcess, atLeastOnce()).getInputStream();
    verify(mockProcess, atLeastOnce()).exitValue();
  }

  /**
   * Success case where process passes the error-check and returns DONE.
   */
  @Test
  public void testProcess_Done() throws IOException
  {
    setUpProcessBuilder("Output: completed normally\nYes we really did\n", EXITCODE_SUCCESS);
    assertEquals(TaskStatus.DONE, localShellTask.process(false));
    verifyProcessBuilder();
  }

  /**
   * Process output is flagged by the configured error regexp.
   */
  @Test
  public void testProcess_RegexpError() throws IOException
  {
    setUpProcessBuilder("Output: An ERROR happened!\nBut still returning successful exitValue!!\n", EXITCODE_SUCCESS);
    assertEquals(TaskStatus.ERROR, localShellTask.process(false));
    verifyProcessBuilder();
  }

  /**
   * Process exitValue is flagged because it does not match the configured exitValue.
   */
  @Test
  public void testProcess_ExitValueFail() throws IOException
  {
    setUpProcessBuilder("Output: Might look ok here...!\nBut exitValue will not match configured success value\n", EXITCODE_ERROR);
    assertEquals(TaskStatus.ERROR, localShellTask.process(false));
    verifyProcessBuilder();
  }

  /**
   * Process exitValue is the standard posix error value but will be deemed ok when there is no configured exitValue.
   */
  @Test
  public void testProcess_ExitValueOk() throws IOException
  {
    setUpProcessBuilder("Output: exitValue is an 'error' but it will pass since there is no configured success value\n", EXITCODE_ERROR);
    shellConfig.setExitvalueSuccess(null);
    assertEquals(TaskStatus.DONE, localShellTask.process(false));
    verifyProcessBuilder();
  }

  /**
   * Noop always returns noop, and at least invokes loadDataModel.
   */
  @Test
  public void testProcessNoop()
  {
    assertEquals(TaskStatus.NOOP, localShellTask.process(true));
    verify(mockTwoEnvStringSubstituter).loadDataModel();
    verifyZeroInteractions(mockProcessBuilderAdapterFactory);
  }
}