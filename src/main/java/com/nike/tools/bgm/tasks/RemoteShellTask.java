package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.substituter.SubstituterResult;
import com.nike.tools.bgm.utils.ShellResult;

/**
 * Runs a command remotely.  The command can have %{variables} substituted using values read from the data model of
 * liveEnv and stageEnv.
 */
@Lazy
@Component
@Scope("prototype")
public class RemoteShellTask extends ShellTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteShellTask.class);

  // TODO - shellConfig should be a new type RemoteShellConfig which extends ShellConfig and has its own SshTarget... currently SshTarget is a singleton
  @Autowired
  private SshTarget sshTarget;

  @Autowired
  private SshClient sshClient;

  /**
   * Runs a command remotely.  Performs substitutions on the %{variable} references in the command string.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    LOGGER.info("Launching remote shell command" + noopRemark(noop));
    forbidCheckingExitValue();
    loadDataModel();
    TaskStatus taskStatus = TaskStatus.NOOP;
    if (!noop)
    {
      checkConfig();
      sshClient.init(sshTarget);
      SubstituterResult command = stringSubstituter.substituteVariables(shellConfig.getCommand());
      LOGGER.info("Executing command '" + command.getExpurgated() + "' on " + sshTarget.getUsername() + "@" + sshTarget.getHostname());
      ShellResult result = sshClient.execCommand(command); //Output available only when completely done.
      taskStatus = checkForErrors(result.getOutput(), result.getExitValue());
      logResults(result, taskStatus);
    }
    return taskStatus;
  }

  /**
   * Unfortunately our ssh library Ganymed does not reliably return an exitValue.
   *
   * @see com.nike.tools.bgm.client.ssh.SshClient#execCommand
   */
  public void forbidCheckingExitValue()
  {
    if (shellConfig.getExitvalueSuccess() != null)
    {
      throw new IllegalArgumentException("RemoteShellTask currently does not support checking exitValue of remote command");
    }
  }

  /**
   * Logs the output and exit value, and remarks if error.
   */
  private void logResults(ShellResult result, TaskStatus taskStatus)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("---------- OUTPUT BEGINS ----------");
      if (StringUtils.isNotBlank(result.getOutput()))
      {
        for (String line : result.getOutput().split("\n"))
        {
          LOGGER.debug(line);
        }
      }
      LOGGER.debug("---------- OUTPUT ENDS ----------");
    }
    logExitValue(result.getExitValue());
    if (taskStatus == TaskStatus.ERROR)
    {
      LOGGER.debug("Output and/or exitValue was deemed an error");
    }
  }
}
