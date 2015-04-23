package com.nike.tools.bgm.tasks;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ProcessBuilderAdapter;
import com.nike.tools.bgm.utils.ProcessBuilderAdapterFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LocalShellConfigTest
{
  private static final String COMMAND = "run some stuff";
  private static final String REGEXP_ERROR = "(An ERROR happened!|Bad stuff)";
  private static final Integer EXITCODE_SUCCESS = 0;
  private static final Integer EXITCODE_ERROR = 1;
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @InjectMocks
  private LocalShellTask localShellTask;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Spy
  private LocalShellConfig localShellConfig = new LocalShellConfig(COMMAND, REGEXP_ERROR, EXITCODE_SUCCESS);

  @Mock
  private ProcessBuilderAdapterFactory mockProcessBuilderAdapterFactory;

  @Mock
  private ProcessBuilderAdapter mockProcessBuilderAdapter;

  @Mock
  private Process mockProcess;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_LIVE_ENV.getEnvName())).thenReturn(FAKE_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_STAGE_ENV.getEnvName())).thenReturn(FAKE_STAGE_ENV);
    localShellTask.init(1, FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName());
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
   * Substitution should support these %{..} variables: liveEnv, stageEnv, applicationVmMap, physicalDbMap.
   */
  @Test
  public void testSubstituteVariables()
  {
    String command = "run stuff with LIVE_ENV=%{liveEnv}; STAGE_ENV=%{stageEnv}; APPLICATION_VM_MAP=%{applicationVmMap}; PHYSICAL_DB_MAP=%{physicalDbMap}!";
    String result = localShellTask.substituteVariables(command);
    assertTrue(result.contains("LIVE_ENV=" + FAKE_LIVE_ENV.getEnvName()));
    assertTrue(result.contains("STAGE_ENV=" + FAKE_STAGE_ENV.getEnvName()));
    ApplicationVm liveApplicationVm = FAKE_LIVE_ENV.getApplicationVms().get(0);
    ApplicationVm stageApplicationVm = FAKE_STAGE_ENV.getApplicationVms().get(0);
    assertTrue(Pattern.compile("APPLICATION_VM_MAP=" + liveApplicationVm.getHostname() + ".*" + stageApplicationVm.getHostname() + ".*;")
        .matcher(result).find());
    PhysicalDatabase livePhysicalDatabase = FAKE_LIVE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase();
    PhysicalDatabase stagePhysicalDatabase = FAKE_STAGE_ENV.getLogicalDatabases().get(0).getPhysicalDatabase();
    assertTrue(Pattern.compile("PHYSICAL_DB_MAP=" + livePhysicalDatabase.getInstanceName() + ".*" + stagePhysicalDatabase.getInstanceName() + "!")
        .matcher(result).find());
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
    localShellConfig.setExitvalueSuccess(null);
    assertEquals(TaskStatus.DONE, localShellTask.process(false));
    verifyProcessBuilder();
  }

  /**
   * Noop always returns noop.  Also implicitly tests init().
   */
  @Test
  public void testProcessNoop()
  {
    assertEquals(TaskStatus.NOOP, localShellTask.process(true));
    verify(mockEnvironmentTx, times(2)).findNamedEnv(anyString());
    verifyZeroInteractions(mockProcessBuilderAdapterFactory);
  }
}