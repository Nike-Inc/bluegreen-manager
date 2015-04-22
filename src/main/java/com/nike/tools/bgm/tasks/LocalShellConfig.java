package com.nike.tools.bgm.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Configures the local shell command.
 */
@Lazy
@Component
public class LocalShellConfig
{
  @Value("${bluegreen.localshell.command}")
  private String command;

  @Value("#{bluegreen.localshell.regexp.error}")
  private String regexpError;

  @Value("#{bluegreen.localshell.exitvalue.success}")
  private Integer exitvalueSuccess;

  public LocalShellConfig()
  {
  }

  public LocalShellConfig(String command, String regexpError, Integer exitvalueSuccess)
  {
    this.command = command;
    this.regexpError = regexpError;
    this.exitvalueSuccess = exitvalueSuccess;
  }

  public String getCommand()
  {
    return command;
  }

  public void setCommand(String command)
  {
    this.command = command;
  }

  public String getRegexpError()
  {
    return regexpError;
  }

  public void setRegexpError(String regexpError)
  {
    this.regexpError = regexpError;
  }

  public Integer getExitvalueSuccess()
  {
    return exitvalueSuccess;
  }

  public void setExitvalueSuccess(Integer exitvalueSuccess)
  {
    this.exitvalueSuccess = exitvalueSuccess;
  }
}
