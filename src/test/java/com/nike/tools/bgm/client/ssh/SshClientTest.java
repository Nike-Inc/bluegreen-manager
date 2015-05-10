package com.nike.tools.bgm.client.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.utils.ShellResult;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the glue logic of SshClient but mocks the Connection.
 */
@RunWith(MockitoJUnitRunner.class)
public class SshClientTest
{
  private static final String HOSTNAME = "ssh-target-hostname.com";
  private static final String USERNAME = "target_user";
  private static final String PASSWORD = "password";
  private static final long TIMEOUT = 10L;
  private static final int TIMEOUT_INT = (int) TIMEOUT;
  private static final String COMMAND = "run the big stuff";
  private static final String STDOUT_STRING = "The first line of output.\nThe second line.\nAnd a third line.\n";
  private static final ShellResult RESULT = new ShellResult(STDOUT_STRING, 0);
  private static final InputStream INPUT_STREAM = IOUtils.toInputStream(STDOUT_STRING);

  @InjectMocks
  private SshClient sshClient;

  @Mock
  private SshConnectionFactory mockSshConnectionFactory;

  @Mock
  private Connection mockConnection;

  @Mock
  private Session mockSession;

  @Before
  public void setUp() throws IOException
  {
    when(mockSshConnectionFactory.create(anyString())).thenReturn(mockConnection);
    when(mockConnection.openSession()).thenReturn(mockSession);
  }

  private void initWithFakeTarget()
  {
    sshClient.init(new SshTarget(HOSTNAME, USERNAME, PASSWORD, TIMEOUT, TIMEOUT));
  }

  /**
   * Sets up the mock result for authentication.
   */
  private void authenticationIsSuccessful(boolean authenticated) throws IOException
  {
    when(mockConnection.authenticateWithPassword(anyString(), anyString())).thenReturn(authenticated);
  }

  /**
   * Initializer should create a new connection, then connect and authenticate.
   */
  @Test
  public void testInit_Pass() throws IOException
  {
    authenticationIsSuccessful(true);
    initWithFakeTarget();
    verify(mockSshConnectionFactory).create(HOSTNAME);
    verify(mockConnection).connect(null, TIMEOUT_INT, TIMEOUT_INT);
    verify(mockConnection).authenticateWithPassword(USERNAME, PASSWORD);
  }

  /**
   * Internal IOExceptions convert to RuntimeException.
   */
  @Test(expected = RuntimeException.class)
  public void testInit_CantConnect() throws IOException
  {
    when(mockConnection.connect(any(ServerHostKeyVerifier.class), anyInt(), anyInt())).thenThrow(new IOException());
    initWithFakeTarget();
  }

  /**
   * When authentication fails, the initializer should throw.
   */
  @Test(expected = RuntimeException.class)
  public void testInit_WrongPassword() throws IOException
  {
    authenticationIsSuccessful(false);
    initWithFakeTarget();
  }

  /**
   * Client executes command and successfully returns output string.
   */
  @Test
  public void testExecCommand_Pass() throws IOException
  {
    when(mockSession.getStdout()).thenReturn(INPUT_STREAM);
    when(mockSession.getExitStatus()).thenReturn(0);
    authenticationIsSuccessful(true);
    initWithFakeTarget();
    assertEquals(RESULT, sshClient.execCommand(COMMAND));
    verify(mockSession).close();
  }

  /**
   * Command failure converts IOException to runtime exception.
   */
  @Test(expected = RuntimeException.class)
  public void testExecCommand_CommandFail() throws IOException
  {
    doThrow(new IOException()).when(mockSession).execCommand(anyString());
    authenticationIsSuccessful(true);
    initWithFakeTarget();
    sshClient.execCommand(COMMAND);
  }
}