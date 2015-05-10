package com.nike.tools.bgm.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Configures the commands sent over ssh for vm deletion.
 * <p/>
 * TODO - This class should be absorbed into a general purpose shell config
 */
@Lazy
@Component
public class SshVmDeleteConfig
{
  @Value("${bluegreen.sshvmdelete.initial.command}")
  private String initialCommand;

  @Value("${bluegreen.sshvmdelete.initial.exitvalue.success}")
  private Integer initialExitvalueSuccess;

  public SshVmDeleteConfig()
  {
  }

  public SshVmDeleteConfig(String initialCommand, Integer initialExitvalueSuccess)
  {
    this.initialCommand = initialCommand;
    this.initialExitvalueSuccess = initialExitvalueSuccess;
  }

  public String getInitialCommand()
  {
    return initialCommand;
  }

  public void setInitialCommand(String initialCommand)
  {
    this.initialCommand = initialCommand;
  }

  public Integer getInitialExitvalueSuccess()
  {
    return initialExitvalueSuccess;
  }

  public void setInitialExitvalueSuccess(Integer initialExitvalueSuccess)
  {
    this.initialExitvalueSuccess = initialExitvalueSuccess;
  }
}
