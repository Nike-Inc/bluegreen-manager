package bluegreen.manager.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.client.ssh.SshClient;
import bluegreen.manager.client.ssh.SshTarget;
import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.EnvironmentTestHelper;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.EnvironmentTx;
import bluegreen.manager.model.tx.OneEnvLoader;
import bluegreen.manager.substituter.OneEnvStringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;
import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.utils.RegexHelper;
import bluegreen.manager.utils.ShellResult;
import static org.junit.Assert.assertEquals;
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
  private static final SubstituterResult INITIAL_CMD_SUBST = new SubstituterResult(INITIAL_CMD, INITIAL_CMD);
  private static final ShellResult DONE_RESULT = new ShellResult("New VM was successfully deleted", 0);
  private static final ShellResult ERROR_RESULT = new ShellResult("No such VM, could not delete", 1);
  private static final String REGEXP_SUCCESS = "successfully";

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
  protected SshVmDeleteConfig fakeSshVmDeleteConfig = new SshVmDeleteConfig(INITIAL_CMD, REGEXP_SUCCESS);

  @Mock
  private SshClient mockSshClient;

  @Spy
  private RegexHelper spyRegexHelper;

  @Mock
  private StringSubstituterFactory mockStringSubstituterFactory;

  @Mock
  private OneEnvStringSubstituter mockOneEnvStringSubstituter;

  private final Environment fullEnv = EnvironmentTestHelper.makeFakeFullEnvironment(0); //_Pass test modifies the env

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(fullEnv.getEnvName())).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(fullEnv);
    when(mockOneEnvLoader.getApplicationVm()).thenReturn(fullEnv.getApplicationVms().get(0));
    when(mockOneEnvLoader.context()).thenReturn("(Context) ");
    when(mockStringSubstituterFactory.createOne(fullEnv.getEnvName(), null)).thenReturn(mockOneEnvStringSubstituter);
    when(mockOneEnvStringSubstituter.substituteVariables(INITIAL_CMD)).thenReturn(INITIAL_CMD_SUBST);
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
    when(mockSshClient.execCommand(INITIAL_CMD_SUBST)).thenReturn(DONE_RESULT);
    assertEquals(TaskStatus.DONE, sshVmDeleteTask.process(false));
    verify(mockOneEnvLoader).loadApplicationVm(false);
    verify(mockSshClient).init(mockSshTarget);
    verify(mockSshClient).execCommand(INITIAL_CMD_SUBST);
    verify(mockEnvironmentTx).updateEnvironment(fullEnv);
  }

  /**
   * Delete command returns a failing exitvalue.
   */
  @Test(expected = RuntimeException.class)
  public void testProcess_Fail()
  {
    when(mockSshClient.execCommand(INITIAL_CMD_SUBST)).thenReturn(ERROR_RESULT);
    sshVmDeleteTask.process(false);
  }
}