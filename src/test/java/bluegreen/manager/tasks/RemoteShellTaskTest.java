package bluegreen.manager.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.client.ssh.SshClient;
import bluegreen.manager.client.ssh.SshTarget;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.substituter.OneEnvStringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;
import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.utils.ShellResult;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemoteShellTaskTest
{
  private static final String COMMAND = "run some stuff";
  private static final SubstituterResult SUBSTITUTED_COMMAND = new SubstituterResult(COMMAND, COMMAND);
  private static final String OUTPUT = "hello, there was SUCCESS";
  private static final String REGEXP_ERROR = "There was an Error";
  private static final int EXIT_VALUE = 0;
  private static final String ENV_NAME = "env1";

  @InjectMocks
  private RemoteShellTask remoteShellTask;

  @Mock
  private SshTarget mockSshTarget;

  @Mock
  private SshClient mockSshClient;

  @Mock
  private StringSubstituterFactory mockStringSubstituterFactory;

  @Mock
  private OneEnvStringSubstituter mockOneEnvStringSubstituter;

  @Before
  public void setUp()
  {
    when(mockStringSubstituterFactory.createOne(anyString(), anyMap())).thenReturn(mockOneEnvStringSubstituter);
    when(mockOneEnvStringSubstituter.substituteVariables(anyString())).thenReturn(SUBSTITUTED_COMMAND);
    when(mockSshClient.execCommand(SUBSTITUTED_COMMAND)).thenReturn(new ShellResult(OUTPUT, EXIT_VALUE));
    remoteShellTask.assign(1, ENV_NAME, new ShellConfig(COMMAND, REGEXP_ERROR, null, null));
  }

  /**
   * Noop returns NOOP and doesn't invoke ssh client.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, remoteShellTask.process(true));
    verifyZeroInteractions(mockSshClient);
  }

  @Test
  public void testProcess_Done()
  {
    assertEquals(TaskStatus.DONE, remoteShellTask.process(false));
    verify(mockSshClient).execCommand(SUBSTITUTED_COMMAND);
  }

}