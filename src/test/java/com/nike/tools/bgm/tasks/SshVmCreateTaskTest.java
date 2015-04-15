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
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationTestHelper;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
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
  private static final Environment FAKE_EMPTY_ENVIRONMENT = ApplicationTestHelper.makeFakeEnvironment();
  private static final String FAKE_EMPTY_ENV_NAME = FAKE_EMPTY_ENVIRONMENT.getEnvName();
  private static final String INITIAL_CMD = "run stuff in env ${envName}";
  private static final String SUBSTITUTED_INITIAL_CMD = "run stuff in env " + FAKE_EMPTY_ENV_NAME;
  private static final String VM_HOSTNAME = "cloudbox1234.hello.com";
  private static final String VM_IPADDRESS = "123.45.67.89";
  private static final String INITIAL_STDOUT = "New VM starting: Hostname=" + VM_HOSTNAME + " IP Address=" + VM_IPADDRESS;
  private static final String INITIAL_REGEXP_IPADDR = "IP Address=(.*)";
  private static final String INITIAL_REGEXP_HOST = "Hostname=(.*)";
  private static final String DONE_FOLLOWUP_STDOUT = "New VM is all DONE";
  private static final String ERROR_FOLLOWUP_STDOUT = "New VM has EXPLODED";
  private static final String NOTDONE_FOLLOWUP_STDOUT = "New VM is still starting up";
  private static final String FOLLOWUP_CMD = "check how ${hostname} is doing";
  private static final String FOLLOWUP_REGEXP_DONE = "all DONE$";
  private static final String FOLLOWUP_REGEXP_ERROR = "EXPLODED$";

  @InjectMocks
  private SshVmCreateTask sshVmCreateTask;

  @Mock
  private ThreadSleeper mockThreadSleeper;

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Mock
  protected SshTarget mockSshTarget;

  @Spy
  protected SshVmCreateConfig fakeSshVmCreateConfig = new SshVmCreateConfig(INITIAL_CMD, INITIAL_REGEXP_IPADDR, INITIAL_REGEXP_HOST,
      FOLLOWUP_CMD, FOLLOWUP_REGEXP_DONE, FOLLOWUP_REGEXP_ERROR);

  @Mock
  private SshClient mockSshClient;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_EMPTY_ENV_NAME)).thenReturn(FAKE_EMPTY_ENVIRONMENT);
    sshVmCreateTask.init(1, FAKE_EMPTY_ENV_NAME);
  }

  /**
   * Tests the case where the followup check shows "not done" three times, followed by a fourth progress object that
   * ends the waiting.  (Technically there are five total progress objects because initial state is also "progress.")
   */
  private void testExecSshVmCreateCommand_ThreeNotDoneThenEnd(String finalStdout,
                                                              Class<? extends Throwable> expectedExceptionType)
      throws InterruptedException
  {
    sshVmCreateTask.setWaitReportInterval(2); //Just to see the extra logging on progress object #2, can't assert it though
    when(mockSshClient.execCommand(anyString()))
        .thenReturn(INITIAL_STDOUT)           //progress #0
        .thenReturn(NOTDONE_FOLLOWUP_STDOUT)  //progress #1, after 1st wait
        .thenReturn(NOTDONE_FOLLOWUP_STDOUT)  //progress #2, after 2nd wait
        .thenReturn(NOTDONE_FOLLOWUP_STDOUT)  //progress #3, after 3rd wait
        .thenReturn(finalStdout);

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

    verify(mockSshClient).execCommand(SUBSTITUTED_INITIAL_CMD);
    verify(mockSshClient, times(5)).execCommand(anyString());
    verify(mockThreadSleeper, times(4)).sleep(anyLong());
  }

  /**
   * Initial output, then three not-done followups, then done.
   */
  @Test
  public void testExecSshVmCreateCommand_ThreeNotDoneThenDone() throws InterruptedException
  {
    testExecSshVmCreateCommand_ThreeNotDoneThenEnd(DONE_FOLLOWUP_STDOUT, null);
  }

  /**
   * Initial output, then three not-done followups, then error - throw.
   */
  @Test
  public void testExecSshVmCreateCommand_ThreeNotDoneThenError() throws InterruptedException
  {
    testExecSshVmCreateCommand_ThreeNotDoneThenEnd(ERROR_FOLLOWUP_STDOUT, RuntimeException.class);
  }

  /**
   * Initial output, then four not-done followups, then timeout.
   */
  @Test
  public void testExecSshVmCreateCommand_FourNotDoneThenTimeout() throws InterruptedException
  {
    sshVmCreateTask.setMaxNumWaits(4);
    testExecSshVmCreateCommand_ThreeNotDoneThenEnd(NOTDONE_FOLLOWUP_STDOUT, RuntimeException.class);
  }

  /**
   * Noop should return status=noop and not invoke sshClient.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, sshVmCreateTask.process(true));
    verifyZeroInteractions(mockSshClient);
  }
}