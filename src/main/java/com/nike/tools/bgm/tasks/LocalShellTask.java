package com.nike.tools.bgm.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ProcessBuilderAdapter;
import com.nike.tools.bgm.utils.ProcessBuilderAdapterFactory;

/**
 * Runs a configurable command locally that requires read access to the data model of liveEnv and stageEnv.
 * Assumes stageEnv vms have been created already.
 * <p/>
 * Command is expected to run in the foreground, and does not provide a "checking service" to report on progress,
 * hence there is no Waiter or ProgressChecker here.  No analysis is performed on the results except to know whether
 * the command succeeded or failed.
 * <p/>
 * Currently no support for timeout.
 */
@Lazy
@Component
public class LocalShellTask extends TaskImpl
{
  /**
   * Variable to be substituted with the name of the live environment.
   */
  private static final String CMDVAR_LIVE_ENV = "%{liveEnv}";

  /**
   * Variable to be substituted with the name of the stage environment.
   */
  private static final String CMDVAR_STAGE_ENV = "%{stageEnv}";

  /**
   * Variable to be substituted with a comma-delimited list of four applicationVm properties:
   * liveHostname,liveIpAddress,stageHostname,stageIpAddress.
   * <p/>
   * Currently only supports mapping 1 vm from live to stage.
   */
  private static final String CMDVAR_APPLICATION_VM_MAP = "%{applicationVmMap}";

  /**
   * Variable to be substituted with a comma-delimited list of two physicaldb properties:
   * livePhysicalInstName,stagePhysicalInstName
   * <p/>
   * Currently only supports mapping 1 physicaldb from live to stage.
   */
  private static final String CMDVAR_PHYSICAL_DB_MAP = "%{physicalDbMap}";

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalShellTask.class);

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private LocalShellConfig localShellConfig;

  @Autowired
  private ProcessBuilderAdapterFactory processBuilderAdapterFactory;

  private Environment liveEnv;
  private ApplicationVm liveApplicationVm;
  private PhysicalDatabase livePhysicalDatabase;
  private Environment stageEnv;
  private ApplicationVm stageApplicationVm;
  private PhysicalDatabase stagePhysicalDatabase;
  private Pattern patternError;

  public Task init(int position, String liveEnvName, String stageEnvName)
  {
    if (StringUtils.equals(liveEnvName, stageEnvName))
    {
      throw new IllegalArgumentException("Live env must be different from stage env, cannot target env '" + liveEnvName + "' for both");
    }
    super.init(position);
    this.liveEnv = environmentTx.findNamedEnv(liveEnvName);
    this.stageEnv = environmentTx.findNamedEnv(stageEnvName);
    this.liveApplicationVm = findApplicationVmFromEnvironment(liveEnv);
    this.stageApplicationVm = findApplicationVmFromEnvironment(stageEnv);
    this.livePhysicalDatabase = findPhysicalDatabaseFromEnvironment(liveEnv);
    this.stagePhysicalDatabase = findPhysicalDatabaseFromEnvironment(stageEnv);
    this.patternError = Pattern.compile(localShellConfig.getRegexpError());
    return this;
  }

  private String context(Environment environment)
  {
    return "[Environment '" + environment.getEnvName() + "']: ";
  }

  /**
   * Gets the env's persisted application vm record.  (Currently support only 1.)
   */
  protected ApplicationVm findApplicationVmFromEnvironment(Environment environment)
  {
    List<ApplicationVm> applicationVms = environment.getApplicationVms();
    if (CollectionUtils.isEmpty(applicationVms))
    {
      throw new IllegalStateException(context(environment) + "No application vms");
    }
    else
    {
      if (applicationVms.size() > 1)
      {
        throw new UnsupportedOperationException(context(environment) + "Currently only support case of 1 applicationVm, but environment '"
            + environment.getEnvName() + "' has " + applicationVms.size());
      }
      return applicationVms.get(0);
    }
  }

  /**
   * Gets the env's persisted physicaldb record.  Requires exactly 1.
   */
  private PhysicalDatabase findPhysicalDatabaseFromEnvironment(Environment environment)
  {
    LogicalDatabase logicalDatabase = findLogicalDatabaseFromEnvironment(environment);
    return logicalDatabase.getPhysicalDatabase();
  }

  /**
   * Gets the env's persisted logicaldb record.  Requires exactly 1.
   */
  private LogicalDatabase findLogicalDatabaseFromEnvironment(Environment environment)
  {
    List<LogicalDatabase> logicalDatabases = environment.getLogicalDatabases();
    if (CollectionUtils.isEmpty(logicalDatabases))
    {
      throw new IllegalStateException(context(environment) + "No logical databases");
    }
    else if (logicalDatabases.size() > 1)
    {
      //TODO - start sharing code with RDSSnapshotRestoreTask et al
      throw new UnsupportedOperationException(context(environment) + "Currently only support case of 1 logicalDatabase, but live env has "
          + logicalDatabases.size()); // + ": " + listOfNames(logicalDatabases));
    }
    else if (StringUtils.isBlank(logicalDatabases.get(0).getLogicalName()))
    {
      throw new IllegalStateException(context(environment) + "Logical database has blank name");
    }
    return logicalDatabases.get(0);
  }

  /**
   * Runs a configurable command locally.
   * <p/>
   * Provides read access to the data model of liveEnv and stageEnv, by means of command token substitution.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    LOGGER.info("Launching local shell command" + noopRemark(noop));
    TaskStatus taskStatus = TaskStatus.NOOP;
    if (!noop)
    {
      String[] commandTokens = substituteVariables(localShellConfig.getCommand()).split("\\s+");
      ProcessBuilderAdapter processBuilderAdapter = processBuilderAdapterFactory.create(commandTokens)
          .redirectErrorStream(true);
      LOGGER.info("Executing command '" + StringUtils.join(commandTokens, " ") + "'");
      StopWatch stopWatch = new StopWatch();
      try
      {
        stopWatch.start();
        Process process = processBuilderAdapter.start();
        String output = blockAndLogOutput(process);
        taskStatus = checkForErrors(output, process.exitValue());
      }
      catch (IOException e)
      {
        LOGGER.error("Shell command failed: " + StringUtils.join(commandTokens, " "), e);
        taskStatus = TaskStatus.ERROR;
      }
      finally
      {
        stopWatch.stop();
        LOGGER.debug("Time elapsed: " + stopWatch);
      }
    }
    return taskStatus;
  }

  /**
   * Substitutes variables of the form '%{vblname}' in the original string, returns the replaced version.
   * Currently supports only the following variables: liveEnv, stageEnv, applicationVmMap
   * <p/>
   * Can't support '${..}' since Spring already substitutes that in properties file.
   */
  String substituteVariables(String command)
  {
    if (StringUtils.isBlank(command))
    {
      throw new IllegalArgumentException("Command is blank");
    }
    String substituted = command;
    substituted = StringUtils.replace(substituted, CMDVAR_LIVE_ENV, liveEnv.getEnvName());
    substituted = StringUtils.replace(substituted, CMDVAR_STAGE_ENV, stageEnv.getEnvName());
    substituted = StringUtils.replace(substituted, CMDVAR_APPLICATION_VM_MAP, makeApplicationVmMapString());
    substituted = StringUtils.replace(substituted, CMDVAR_PHYSICAL_DB_MAP, makePhysicalDbMapString());
    return substituted;
  }

  /**
   * Makes a comma-delimited list of four applicationVm properties:
   * liveHostname,liveIpAddress,stageHostname,stageIpAddress.
   * <p/>
   * Currently only supports mapping 1 vm from live to stage.
   */
  private String makeApplicationVmMapString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(liveApplicationVm.getHostname());
    sb.append(",");
    sb.append(liveApplicationVm.getIpAddress());
    sb.append(",");
    sb.append(stageApplicationVm.getHostname());
    sb.append(",");
    sb.append(stageApplicationVm.getIpAddress());
    return sb.toString();
  }

  /**
   * Makes a comma-delimited list of two physicaldb properties:
   * livePhysicalInstName,stagePhysicalInstName
   * <p/>
   * Currently only supports mapping 1 physicaldb from live to stage.
   */
  private String makePhysicalDbMapString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(livePhysicalDatabase.getInstanceName());
    sb.append(",");
    sb.append(stagePhysicalDatabase.getInstanceName());
    return sb.toString();
  }

  /**
   * Iterates over the process stdout until there is no more.  Blocks til the process is done.
   * Returns the output as a single string.
   * <p/>
   * Also logs the process exit value.
   */
  private String blockAndLogOutput(Process process) throws IOException
  {
    // Yes, stdout is 'getInputStream'.
    StringBuilder sb = new StringBuilder();
    LOGGER.debug("---------- OUTPUT BEGINS ----------");
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null)
    {
      LOGGER.debug(line);
      sb.append(line + "\n");
    }
    LOGGER.debug("---------- OUTPUT ENDS ----------");
    logExitValue(process.exitValue());
    return sb.toString();
  }

  /**
   * Checks the process output and exitValue and returns DONE (success) or ERROR.
   */
  private TaskStatus checkForErrors(String output, int exitValue)
  {
    return checkOutput(output) && checkExitValue(exitValue) ? TaskStatus.DONE : TaskStatus.ERROR;
  }

  /**
   * True if output looks ok, false if it matches the error-regexp.
   */
  private boolean checkOutput(String output)
  {
    return !patternError.matcher(output).find();
  }

  /**
   * True if exitValue looks like success, or if success hasn't been defined.
   * <p/>
   * False only if success was defined and this exitValue is something else.
   */
  private boolean checkExitValue(int exitValue)
  {
    return localShellConfig.getExitvalueSuccess() == null || localShellConfig.getExitvalueSuccess() == exitValue;
  }

  /**
   * Logs the exit value, remarking on success/failure if the local shell config has defined the success value.
   */
  private void logExitValue(int exitValue)
  {
    if (localShellConfig.getExitvalueSuccess() == null)
    {
      LOGGER.debug("Command exit code: " + exitValue);
    }
    else
    {
      boolean success = localShellConfig.getExitvalueSuccess() == exitValue;
      LOGGER.debug("Command exit code: " + exitValue + " " + (success ? "(success)" : "(failure)"));
    }
  }
}
