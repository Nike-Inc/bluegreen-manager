package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshClientResult;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.EnvironmentTx;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests processing of configurable ssh commands for VM Deletion.
 */
@RunWith(MockitoJUnitRunner.class)
public class SshVmDeleteTaskTest
{
  private static final String INITIAL_CMD = "delete stuff in env %{envName}";
  private static final Integer INITIAL_EXITVALUE_SUCCESS = 0;
  private static final SshClientResult DONE_RESULT = new SshClientResult("New VM was successfully deleted", 0);
  private static final SshClientResult ERROR_RESULT = new SshClientResult("No such VM, could not delete", 1);

  @InjectMocks
  private SshVmDeleteTask sshVmDeleteTask;

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Mock
  protected SshTarget mockSshTarget;

  @Spy
  protected SshVmDeleteConfig fakeSshVmDeleteConfig = new SshVmDeleteConfig(INITIAL_CMD, INITIAL_EXITVALUE_SUCCESS);

  @Mock
  private SshClient mockSshClient;

  private final Environment fullEnv = EnvironmentTestHelper.makeFakeFullEnvironment(0); //_Pass test modifies the env

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(fullEnv.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(fullEnv);
    when(mockOneEnvLoader.context()).thenReturn("(Context) ");
    sshVmDeleteTask.init(1, fullEnv.getEnvName());
  }

  /**
   * Noop should return status=noop and not invoke sshClient.
   */
  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, sshVmDeleteTask.process(true));
    verifyZeroInteractions(mockSshClient);
  }

  /**
   * Case that passes through every step of process() and passes, returning DONE.
   */
  @Test
  public void testProcess_Pass()
  {
    when(mockSshClient.execCommand(anyString())).thenReturn(DONE_RESULT);
    assertEquals(TaskStatus.DONE, sshVmDeleteTask.process(false));
    verify(mockOneEnvLoader).loadApplicationVm(false);
    verify(mockSshClient).init(mockSshTarget);
    verify(mockSshClient).execCommand(anyString());
    verify(mockEnvironmentTx).updateEnvironment(fullEnv);
  }

  /**
   * Delete command returns a failing exitvalue.
   */
  @Test(expected = RuntimeException.class)
  public void testProcess_Fail()
  {
    when(mockSshClient.execCommand(anyString())).thenReturn(ERROR_RESULT);
    sshVmDeleteTask.process(false);
  }
}