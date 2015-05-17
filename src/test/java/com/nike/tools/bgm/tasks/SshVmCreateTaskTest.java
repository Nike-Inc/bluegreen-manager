package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.model.tx.OneEnvLoader;
import com.nike.tools.bgm.substituter.OneEnvStringSubstituter;
import com.nike.tools.bgm.substituter.StringSubstituterFactory;
import com.nike.tools.bgm.substituter.SubstituterResult;
import com.nike.tools.bgm.substituter.ZeroEnvStringSubstituter;
import com.nike.tools.bgm.utils.RegexHelper;
import com.nike.tools.bgm.utils.ShellResult;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.WaiterParameters;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests processing of configurable ssh commands for VM Creation.
 */
@RunWith(MockitoJUnitRunner.class)
public class SshVmCreateTaskTest
{
  private static final Environment FAKE_EMPTY_ENVIRONMENT = EnvironmentTestHelper.makeFakeEnvironment();
  private static final String FAKE_EMPTY_ENV_NAME = FAKE_EMPTY_ENVIRONMENT.getEnvName();
  private static final String INITIAL_CMD_TEMPLATE = "run stuff in env %{envName}";
  private static final String INITIAL_CMD = "run stuff in env " + FAKE_EMPTY_ENV_NAME;
  private static final String VM_HOSTNAME = "cloudbox1234.hello.com";
  private static final String VM_IPADDRESS = "123.45.67.89";
  private static final ShellResult INITIAL_RESULT = new ShellResult("New VM starting: Hostname=" + VM_HOSTNAME + " IP Address=" + VM_IPADDRESS, 0);
  private static final String INITIAL_REGEXP_IPADDR = "IP Address=(.*)";
  private static final String INITIAL_REGEXP_HOST = "Hostname=(.*)";
  private static final ShellResult DONE_FOLLOWUP_RESULT = new ShellResult("New VM is all DONE", 0);
  private static final ShellResult ERROR_FOLLOWUP_RESULT = new ShellResult("New VM has EXPLODED", 1);
  private static final ShellResult NOTDONE_FOLLOWUP_RESULT = new ShellResult("New VM is still starting up", 0);
  private static final String FOLLOWUP_CMD = "check how %{hostname} is doing";
  private static final String FOLLOWUP_REGEXP_DONE = "all DONE$";
  private static final String FOLLOWUP_REGEXP_ERROR = "EXPLODED$";

  @InjectMocks
  private SshVmCreateTask sshVmCreateTask;

  @Spy
  protected WaiterParameters fakeWaiterParameters = new WaiterParameters(10L, 10L, 2, 20);

  @Mock
  private ThreadSleeper mockThreadSleeper;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Mock
  protected SshTarget mockSshTarget;

  @Spy
  protected SshVmCreateConfig fakeSshVmCreateConfig = new SshVmCreateConfig(INITIAL_CMD_TEMPLATE, INITIAL_REGEXP_IPADDR, INITIAL_REGEXP_HOST,
      FOLLOWUP_CMD, FOLLOWUP_REGEXP_DONE, FOLLOWUP_REGEXP_ERROR);

  @Mock
  private SshClient mockSshClient;

  @Spy
  private RegexHelper spyRegexHelper;

  @Mock
  private StringSubstituterFactory mockStringSubstituterFactory;

  @Mock
  private OneEnvStringSubstituter mockOneEnvStringSubstituter;

  @Mock
  private ZeroEnvStringSubstituter mockZeroEnvStringSubstituter;

  @Mock
  private SubstituterResult mockSubstituterResult;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(FAKE_EMPTY_ENV_NAME)).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(FAKE_EMPTY_ENVIRONMENT);
    when(mockOneEnvLoader.context()).thenReturn("(Context) ");
    when(mockStringSubstituterFactory.createOne(anyString(), anyMap())).thenReturn(mockOneEnvStringSubstituter);
    when(mockStringSubstituterFactory.createZero(anyMap())).thenReturn(mockZeroEnvStringSubstituter);
    when(mockOneEnvStringSubstituter.substituteVariables(anyString())).thenReturn(mockSubstituterResult);
    when(mockZeroEnvStringSubstituter.substituteVariables(anyString())).thenReturn(mockSubstituterResult);
    sshVmCreateTask.init(1, FAKE_EMPTY_ENV_NAME);
    sshVmCreateTask.loadDataModel();
  }

  /**
   * Tests the case where the followup check shows "not done" three times, followed by a fourth progress object that
   * ends the waiting.  (Technically there are five total progress objects because initial state is also "progress.")
   */
  private void testExecSshVmCreateCommand_ThreeNotDoneThenEnd(ShellResult finalResult,
                                                              Class<? extends Throwable> expectedExceptionType)
      throws InterruptedException
  {
    when(mockSshClient.execCommand(mockSubstituterResult))
        .thenReturn(INITIAL_RESULT)           //progress #0
        .thenReturn(NOTDONE_FOLLOWUP_RESULT)  //progress #1, after 1st wait
        .thenReturn(NOTDONE_FOLLOWUP_RESULT)  //progress #2, after 2nd wait
        .thenReturn(NOTDONE_FOLLOWUP_RESULT)  //progress #3, after 3rd wait
        .thenReturn(finalResult);

    RuntimeException exception = null;
    try
    {
      sshVmCreateTask.execSshVmCreateCommand(false);
    }
    catch (Throwable e)
    {
      if (expectedExceptionType == null || !expectedExceptionType.isAssignableFrom(e.getClass()))
      {
        throw new RuntimeException("Unexpected exception", e);
      }
    }

    verify(mockSshClient, times(5)).execCommand(mockSubstituterResult);
    verify(mockThreadSleeper, times(4)).sleep(anyLong());
  }

  /**
   * Initial output, then three not-done followups, then done.
   */
  @Test
  public void testExecSshVmCreateCommand_ThreeNotDoneThenDone() throws InterruptedException
  {
    testExecSshVmCreateCommand_ThreeNotDoneThenEnd(DONE_FOLLOWUP_RESULT, null);
  }

  /**
   * Initial output, then three not-done followups, then error - throw.
   */
  @Test
  public void testExecSshVmCreateCommand_ThreeNotDoneThenError() throws InterruptedException
  {
    testExecSshVmCreateCommand_ThreeNotDoneThenEnd(ERROR_FOLLOWUP_RESULT, RuntimeException.class);
  }

  /**
   * Initial output, then four not-done followups, then timeout.
   */
  @Test
  public void testExecSshVmCreateCommand_FourNotDoneThenTimeout() throws InterruptedException
  {
    fakeWaiterParameters.setMaxNumWaits(4);
    testExecSshVmCreateCommand_ThreeNotDoneThenEnd(NOTDONE_FOLLOWUP_RESULT, RuntimeException.class);
  }

  /**
   * Noop should return status=noop and not invoke sshClient.
   * <p/>
   * As the only test to invoke process(), this results in calling loadDataModel() twice. Should be ok.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, sshVmCreateTask.process(true));
    verifyZeroInteractions(mockSshClient);
  }
}