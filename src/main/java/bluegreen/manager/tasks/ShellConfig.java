package bluegreen.manager.tasks;

import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Configures a shell command: the command template, variable substitutions, regular expression to detect errors,
 * expected command exit value.
 */
@Lazy
@Component
public class ShellConfig
{
  /**
   * Shell command, which may include instances of "%{variable}".
   */
  private String command;

  /**
   * Optional regular expression which, if matched in the command output, would indicate the command ended in error.
   */
  private String regexpError;

  /**
   * Optional exit value which, if specified, must match the shell return code to indicate the command ended in success.
   */
  private Integer exitvalueSuccess;

  /**
   * Optional map from 'variable' to 'replacement-value'.  e.g. Map('hello', 'world') applied to command
   * "doStuff --arg %{hello}" would result in a shell command "doStuff --arg world".
   * <p/>
   * See usage in StringSubstituter class hierarchy.
   */
  private Map<String, String> extraSubstitutions;

  public ShellConfig()
  {
  }

  public ShellConfig(String command,
                     String regexpError,
                     Integer exitvalueSuccess,
                     Map<String, String> extraSubstitutions)
  {
    this.command = command;
    this.regexpError = regexpError;
    this.exitvalueSuccess = exitvalueSuccess;
    this.extraSubstitutions = extraSubstitutions;
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

  public Map<String, String> getExtraSubstitutions()
  {
    return extraSubstitutions;
  }

  public void setExtraSubstitutions(Map<String, String> extraSubstitutions)
  {
    this.extraSubstitutions = extraSubstitutions;
  }
}
