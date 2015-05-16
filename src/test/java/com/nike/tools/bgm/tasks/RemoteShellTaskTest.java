package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.substituter.OneEnvStringSubstituter;
import com.nike.tools.bgm.substituter.StringSubstituterFactory;
import com.nike.tools.bgm.substituter.SubstituterResult;
import com.nike.tools.bgm.utils.ShellResult;

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
    when(mockSshClient.execCommand(SUBSTITUTED_COMMAND)).thenReturn(new ShellResult("hello", 0));
    remoteShellTask.assign(1, "env1", new ShellConfig(COMMAND, null, 0, null));
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