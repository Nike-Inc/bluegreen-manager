package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests ability to check progress of Vm Creation using some example ssh commands.
 */
@RunWith(MockitoJUnitRunner.class)
public class SshVmCreateProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context) ";
  private static final int WAIT_NUM = 1;
  private static final String SSH_HOSTNAME = "ssh-target-hostname.com";
  private static final String SSH_USERNAME = "target_user";
  private static final String SSH_PASSWORD = "password";
  private static final long SSH_TIMEOUT = 10L;
  private static final SshTarget FAKE_SSH_TARGET = new SshTarget(SSH_HOSTNAME, SSH_USERNAME, SSH_PASSWORD, SSH_TIMEOUT, SSH_TIMEOUT);
  private static final String VM_HOSTNAME = "cloudbox1234.hello.com";
  private static final String VM_IPADDRESS = "123.45.67.89";
  private static final String GOOD_INITIAL_STDOUT = "New VM started:\nHostname=" + VM_HOSTNAME + "\nIP Address=" + VM_IPADDRESS + "\n";
  private static final String BAD_INITIAL_STDOUT = "New VM started:\nHostname=" + VM_HOSTNAME + "\nNo IP Address\n";
  private static final String INITIAL_CMD = "run stuff";
  private static final String INITIAL_REGEXP_IPADDR = "^IP Address=(.*)";
  private static final String INITIAL_REGEXP_HOST = "^Hostname=(.*)";
  private static final String DONE_FOLLOWUP_STDOUT = "VM is all done!\nThe End";
  private static final String ERROR_FOLLOWUP_STDOUT = "Error starting your VM";
  private static final String NOTDONE_FOLLOWUP_STDOUT = "VM is still starting up";
  private static final String FOLLOWUP_CMD = "check how %{hostname} is doing";
  private static final String SUBSTITUTED_FOLLOWUP_CMD = "check how " + VM_HOSTNAME + " is doing";
  private static final String FOLLOWUP_REGEXP_DONE = "all done";
  private static final String FOLLOWUP_REGEXP_ERROR = "[Ee]rror";
  private static final SshVmCreateConfig FAKE_CONFIG = new SshVmCreateConfig(INITIAL_CMD, INITIAL_REGEXP_IPADDR, INITIAL_REGEXP_HOST,
      FOLLOWUP_CMD, FOLLOWUP_REGEXP_DONE, FOLLOWUP_REGEXP_ERROR);

  @Mock
  private SshClient mockSshClient;

  private SshVmCreateProgressChecker makeProgressChecker(String initialOutput, SshVmCreateConfig sshVmCreateConfig)
  {
    return new SshVmCreateProgressChecker(initialOutput, LOG_CONTEXT, mockSshClient, FAKE_SSH_TARGET, sshVmCreateConfig);
  }

  /**
   * Description contains ssh target's username and hostname.
   */
  @Test
  public void testGetDescription()
  {
    SshVmCreateProgressChecker progressChecker = makeProgressChecker(GOOD_INITIAL_STDOUT, FAKE_CONFIG);
    String description = progressChecker.getDescription();
    assertTrue(description.contains(SSH_USERNAME));
    assertTrue(description.contains(SSH_HOSTNAME));
  }

  /**
   * Initial check fails if no initial output.
   */
  @Test(expected = RuntimeException.class)
  public void testInitialCheck_BlankOutput()
  {
    SshVmCreateProgressChecker progressChecker = makeProgressChecker("", FAKE_CONFIG);
    progressChecker.initialCheck();
  }

  /**
   * Initial check fails if initial output lacks a required capture string.
   */
  @Test(expected = RuntimeException.class)
  public void testInitialCheck_MissingRequiredCapture()
  {
    SshVmCreateProgressChecker progressChecker = makeProgressChecker(BAD_INITIAL_STDOUT, FAKE_CONFIG);
    progressChecker.initialCheck();
  }

  /**
   * Initial check with valid initial output.
   */
  @Test
  public void testInitialCheck_Pass()
  {
    SshVmCreateProgressChecker progressChecker = makeProgressChecker(GOOD_INITIAL_STDOUT, FAKE_CONFIG);
    progressChecker.initialCheck();
    assertEquals(VM_HOSTNAME, progressChecker.getHostname());
    assertEquals(VM_IPADDRESS, progressChecker.getIpAddress());
  }

  /**
   * Run ok through initialCheck, then try a followup which may or may not work.
   */
  private SshVmCreateProgressChecker testFollowupCheck(String followupStdout)
  {
    when(mockSshClient.execCommand(anyString())).thenReturn(followupStdout);
    SshVmCreateProgressChecker progressChecker = makeProgressChecker(GOOD_INITIAL_STDOUT, FAKE_CONFIG);
    progressChecker.initialCheck();
    progressChecker.followupCheck(WAIT_NUM);
    return progressChecker;
  }

  /**
   * Verifies that the mock was asked to execute the substituted followup command.
   */
  private void verifyFollowupCommand()
  {
    verify(mockSshClient).execCommand(SUBSTITUTED_FOLLOWUP_CMD);
  }

  /**
   * Followup command emits stdout with no special done/error strings, so we must not be done yet.
   */
  @Test
  public void testFollowupCheck_NotDone()
  {
    SshVmCreateProgressChecker progressChecker = testFollowupCheck(NOTDONE_FOLLOWUP_STDOUT);
    assertFalse(progressChecker.isDone());
    assertNull(progressChecker.getResult());
    verifyFollowupCommand();
  }

  /**
   * Followup command emits stdout with no special done/error strings, so we must not be done yet.
   */
  @Test(expected = RuntimeException.class)
  public void testFollowupCheck_Error()
  {
    testFollowupCheck(ERROR_FOLLOWUP_STDOUT);
  }

  @Test
  public void testFollowupCheck_Done()
  {
    SshVmCreateProgressChecker progressChecker = testFollowupCheck(DONE_FOLLOWUP_STDOUT);
    assertTrue(progressChecker.isDone());
    assertEquals(VM_HOSTNAME, progressChecker.getResult().getHostname());
    assertEquals(VM_IPADDRESS, progressChecker.getResult().getIpAddress());
    verifyFollowupCommand();
  }
}