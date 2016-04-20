package bluegreen.manager.tasks;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.substituter.StringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;

public abstract class ShellTask extends TaskImpl
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ShellTask.class);

  @Autowired
  private StringSubstituterFactory stringSubstituterFactory;

  protected ShellConfig shellConfig;
  protected Pattern patternError;
  protected StringSubstituter stringSubstituter;

  /**
   * Two-env shell task.
   *
   * @param isStageBuilt - true if the stage environment is completely registered in the discovery DB, minus the Application
   */
  public Task assign(int position,
                     String liveEnvName,
                     String stageEnvName,
                     ShellConfig shellConfig,
                     boolean isStageBuilt)
  {
    super.assign(position);
    this.shellConfig = shellConfig;
    if (StringUtils.isNotBlank(shellConfig.getRegexpError()))
    {
      this.patternError = Pattern.compile(shellConfig.getRegexpError());
    }

    if (isStageBuilt) {
      this.stringSubstituter = stringSubstituterFactory.createTwo(liveEnvName, stageEnvName,
          shellConfig.getExtraSubstitutions());
    } else {
      this.stringSubstituter = stringSubstituterFactory.createOnePointFive(liveEnvName, stageEnvName,
          shellConfig.getExtraSubstitutions());
    }
    return this;
  }

  /**
   * One-env shell task.
   */
  public Task assign(int position, String envName, ShellConfig shellConfig)
  {
    super.assign(position);
    this.shellConfig = shellConfig;
    if (StringUtils.isNotBlank(shellConfig.getRegexpError()))
    {
      this.patternError = Pattern.compile(shellConfig.getRegexpError());
    }
    this.stringSubstituter = stringSubstituterFactory.createOne(envName, shellConfig.getExtraSubstitutions());
    return this;
  }

  protected void loadDataModel()
  {
    stringSubstituter.loadDataModel();
  }

  /**
   * Checks that the shell config gives a way to verify success or failure of the process run.
   */
  protected void checkConfig()
  {
    if (shellConfig.getExitvalueSuccess() == null && StringUtils.isBlank(shellConfig.getRegexpError()))
    {
      throw new IllegalArgumentException("Configuration should specify one or both of exitvalueSuccess and regexpError");
    }
  }

  /**
   * Checks the process output and exitValue and returns DONE (success) or ERROR.
   */
  protected TaskStatus checkForErrors(String output, int exitValue)
  {
    return checkOutput(output) && checkExitValue(exitValue) ? TaskStatus.DONE : TaskStatus.ERROR;
  }

  /**
   * True if output looks ok, or if error-regexp hasn't been defined.
   * False if output matches the error-regexp.
   */
  protected boolean checkOutput(String output)
  {
    return patternError == null || !patternError.matcher(output).find();
  }

  /**
   * True if exitValue looks like success, or if success hasn't been defined.
   * <p/>
   * False only if success was defined and this exitValue is something else.
   */
  protected boolean checkExitValue(int exitValue)
  {
    return shellConfig.getExitvalueSuccess() == null || shellConfig.getExitvalueSuccess() == exitValue;
  }

  /**
   * Logs the exit value, remarking on success/failure if the local shell config has defined the success value.
   */
  protected void logExitValue(int exitValue)
  {
    if (shellConfig.getExitvalueSuccess() == null)
    {
      LOGGER.debug("Command exit code: " + exitValue);
    }
    else
    {
      boolean success = shellConfig.getExitvalueSuccess() == exitValue;
      LOGGER.debug("Command exit code: " + exitValue + " " + (success ? "(success)" : "(failure)"));
    }
  }

}
