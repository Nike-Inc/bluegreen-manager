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

  @Value("${bluegreen.sshvmdelete.initial.regexp.success}")
  private String initialRegexpSuccess;

  //Warning: don't try to examine exitValue for success/failure, our ssh library Ganymed does not reliably return it.

  public SshVmDeleteConfig()
  {
  }

  public SshVmDeleteConfig(String initialCommand, String initialRegexpSuccess)
  {
    this.initialCommand = initialCommand;
    this.initialRegexpSuccess = initialRegexpSuccess;
  }

  public String getInitialCommand()
  {
    return initialCommand;
  }

  public void setInitialCommand(String initialCommand)
  {
    this.initialCommand = initialCommand;
  }

  public String getInitialRegexpSuccess()
  {
    return initialRegexpSuccess;
  }

  public void setInitialRegexpSuccess(String initialRegexpSuccess)
  {
    this.initialRegexpSuccess = initialRegexpSuccess;
  }
}
