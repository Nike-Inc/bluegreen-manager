package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.main.ArgumentParser;
import com.nike.tools.bgm.main.CmdlineException;
import com.nike.tools.bgm.model.domain.JobHistory;

/**
 * Given a job name and parameters, returns a Job for processing.
 */
@Component
public class JobFactory
{
  private static Logger LOGGER = LoggerFactory.getLogger(JobFactory.class);

  public static final String JOBNAME_STAGING_DEPLOY = "stagingDeploy";
  public static final String JOBNAME_GO_LIVE = "goLive";
  public static final String JOBNAME_TEARDOWN = "teardown";

  public static final String PARAMNAME_LIVE_ENV = "liveEnv";
  public static final String PARAMNAME_STAGE_ENV = "stageEnv";
  public static final String PARAMNAME_PKGNAMES = "pkgnames";
  public static final String PARAMNAME_OLD_LIVE_ENV = "oldLiveEnv";
  public static final String PARAMNAME_NEW_LIVE_ENV = "newLiveEnv";
  public static final String PARAMNAME_OLD_ENV = "oldEnv";
  public static final String PARAMNAME_NOOP = "noop";
  public static final String PARAMNAME_FORCE = "force";

  private static final long MAX_AGE_RELEVANT_PRIOR_JOB = 1000L * 60L * 60L * 24L; //1 day
  private static final int UNLIMITED_NUM_VALUES = -1;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private JobHistoryTx jobHistoryTx;

  @Autowired
  private EnvironmentTx environmentTx;

  /**
   * Logs an explanation of valid jobs and their expected parameters.
   */
  public void explainValidJobs()
  {
    LOGGER.info("Invoke as follows:\n" + makeExplanationOfValidJobs());
  }

  /**
   * Returns a string explanation of valid jobs and their expected parameters.
   */
  protected String makeExplanationOfValidJobs()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("BlueGreenManager argument format: <jobName> <parameters>\n");
    sb.append("\n");
    sb.append("Job '" + JOBNAME_STAGING_DEPLOY + "'\n");
    sb.append("Description: Spins up and deploys to a new stage env.  Temporarily freezes the live env.\n");
    sb.append("Required Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_LIVE_ENV + " <envName>\n");
    sb.append("\t\t\tSpecify the live env so we can freeze it, replicate its db, and base stage from it.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_STAGE_ENV + " <envName>\n");
    sb.append("\t\t\tThe stage env is where we will spin up a new vm, a test db copy, and deploy packages.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_PKGNAMES + " <list of stage pkgs>\n");
    sb.append("\t\t\tList of packages to deploy to stage, which are DIFFERENT from what is on the live env.\n");
    sb.append("\t\t\tUse full package names, up to the 'tar.gz' suffix.\n");
    sb.append("\n");
    sb.append("Job '" + JOBNAME_GO_LIVE + "'\n");
    sb.append("Description: Reassigns liveness from the old env to the new env.  Old env stays frozen.\n");
    sb.append("Required Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_OLD_LIVE_ENV + " <envName>\n");
    sb.append("\t\t\tSpecify the live env so we can freeze it, replicate its db, and base stage from it.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_NEW_LIVE_ENV + " <envName>\n");
    sb.append("\n");
    sb.append("Job '" + JOBNAME_TEARDOWN + "'\n");
    sb.append("Description: Spins down and destroys the target env.\n");
    sb.append("Required Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_OLD_ENV + " <envName>\n");
    sb.append("\n");
    sb.append("Common Optional Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_NOOP + "\n");
    sb.append("\t\t\tNo-op means print out what this job WOULD do, without taking any actual action.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_FORCE + "\n");
    sb.append("\t\t\tForce job to attempt all tasks, instead of skipping tasks that were successful in the last recent try.\n");
    sb.append("\n");
    return sb.toString();
  }

  /**
   * Constructs a new job using the specified name and parameters, or throws if invalid.
   */
  public Job makeJob(String jobName, List<List<String>> parameters, String commandLine)
  {
    if (jobName != null)
    {
      if (jobName.equals(JOBNAME_STAGING_DEPLOY))
      {
        return makeStagingDeployJob(parameters, commandLine);
      }
      else if (jobName.equals(JOBNAME_GO_LIVE))
      {
        return makeGoLiveJob(parameters, commandLine);
      }
      else if (jobName.equals(JOBNAME_TEARDOWN))
      {
        return makeTeardownJob(parameters, commandLine);
      }
    }
    throw new CmdlineException("Unrecognized jobName: " + jobName);
  }

  /**
   * Constructs a new StagingDeployJob with the specified parameters.
   */
  private Job makeStagingDeployJob(List<List<String>> parameters, String commandLine)
  {
    List<String> pkgnames = getParameterValues(PARAMNAME_PKGNAMES, parameters);
    return makeGenericJob(StagingDeployJob.class, parameters, commandLine, PARAMNAME_LIVE_ENV, PARAMNAME_STAGE_ENV, pkgnames);
  }

  /**
   * Constructs a new GoLiveJob with the specified parameters.
   */
  private Job makeGoLiveJob(List<List<String>> parameters, String commandLine)
  {
    return makeGenericJob(GoLiveJob.class, parameters, commandLine, PARAMNAME_OLD_LIVE_ENV, PARAMNAME_NEW_LIVE_ENV);
  }

  /**
   * Constructs a new TeardownJob with the specified parameters.
   */
  private Job makeTeardownJob(List<List<String>> parameters, String commandLine)
  {
    return makeGenericJob(TeardownJob.class, parameters, commandLine, PARAMNAME_OLD_ENV, null);
  }

  /**
   * Constructs a new Job implementation with the specified parameters.
   * <p/>
   * Looks for boilerplate arguments: noop, force, env1, env2.  Verifies the environment names exist in the database.
   * Also obtains the last relevant job history, if any.
   */
  private Job makeGenericJob(Class<? extends TaskSequenceJob> jobClass,
                             List<List<String>> parameters,
                             String commandLine,
                             String env1ParamName,
                             String env2ParamName,
                             Object... otherArgs)
  {
    boolean noop = hasParameter(PARAMNAME_NOOP, parameters);
    boolean force = hasParameter(PARAMNAME_FORCE, parameters);
    String env1 = env1ParamName == null ? null : getParameter(env1ParamName, parameters, 1).get(1);
    String env2 = env2ParamName == null ? null : getParameter(env2ParamName, parameters, 1).get(1);
    verifyEnvNames(env1, env2);
    JobHistory oldJobHistory = jobHistoryTx.findLastRelevantJobHistory(
        jobClass.getSimpleName(), env1, env2, commandLine, noop, MAX_AGE_RELEVANT_PRIOR_JOB);
    Object[] allArgs = combineKnownArgsWithOtherArgs(commandLine, noop, force, oldJobHistory,
        env1, env2, otherArgs);
    return applicationContext.getBean(jobClass, allArgs);
  }

  /**
   * Returns an array of objects with all the args needed to construct a job.
   */
  private Object[] combineKnownArgsWithOtherArgs(String commandLine,
                                                 boolean noop,
                                                 boolean force,
                                                 JobHistory oldJobHistory,
                                                 String env1,
                                                 String env2,
                                                 Object... otherArgs)
  {
    Object[] allArgs = new Object[6 + otherArgs.length];
    allArgs[0] = commandLine;
    allArgs[1] = noop;
    allArgs[2] = force;
    allArgs[3] = oldJobHistory;
    allArgs[4] = env1;
    allArgs[5] = env2;
    System.arraycopy(otherArgs, 0, allArgs, 6, otherArgs.length);
    return allArgs;
  }

  /**
   * Finds the named parameter in the outer list, or throws.
   * <p/>
   * Each parameter is a sublist of parameter parts.  (ParamName, val1, val2, ...)
   * Asserts that it has the expected number of values (if specified as a non-negative number).
   */
  private List<String> getParameter(String paramName, List<List<String>> parameters, int expectedNumValues)
  {
    if (paramName != null && parameters != null)
    {
      for (List<String> parameter : parameters)
      {
        if (parameter != null && parameter.get(0) != null && StringUtils.equals(paramName, parameter.get(0)))
        {
          if (expectedNumValues == UNLIMITED_NUM_VALUES || parameter.size() == 1 + expectedNumValues)
          {
            return parameter;
          }
          else
          {
            throw new CmdlineException("Parameter '" + paramName + "' expects " + expectedNumValues + " values, but found "
                + (parameter.size() - 1));
          }
        }
      }
    }
    throw new CmdlineException("Missing required parameter '" + paramName + "'");
  }

  /**
   * Returns the values of the named parameter in the outer list, or throws if not found.
   * <p/>
   * Each parameter is a sublist of parameter parts.  (ParamName, val1, val2, ...)
   * In this case returns (val1, val2, ...)
   */
  private List<String> getParameterValues(String paramName, List<List<String>> parameters)
  {
    List<String> parameter = getParameter(paramName, parameters, UNLIMITED_NUM_VALUES);
    return new ArrayList<String>(parameter.subList(1, parameter.size() - 1)); //i.e. skip item#0 (paramName)
  }

  /**
   * Returns true if the named parameter was specified.
   */
  private boolean hasParameter(String paramName, List<List<String>> parameters)
  {
    if (paramName != null && parameters != null)
    {
      for (List<String> parameter : parameters)
      {
        if (parameter != null && parameter.get(0) != null && StringUtils.equals(paramName, parameter.get(0)))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks that all the specified envNames exist in the environment table.  Throws exception if any don't.
   * Returns silently if they all exist.
   */
  private void verifyEnvNames(String... envNames)
  {
    if (envNames == null || envNames.length == 0 || envNames.length > 2)
    {
      throw new IllegalArgumentException();
    }
    boolean[] exists = environmentTx.checkIfEnvNamesExist(envNames);
    if (exists == null || exists.length != envNames.length)
    {
      throw new RuntimeException("env name check returned wrong number of results: "
          + (exists == null ? "null" : exists.length));
    }
    List<String> invalidEnvNames = new ArrayList<String>();
    for (int idx = 0; idx < exists.length; ++idx)
    {
      if (!exists[idx])
      {
        invalidEnvNames.add(envNames[idx]);
      }
    }
    if (invalidEnvNames.size() > 0)
    {
      throw new CmdlineException("Invalid environment names: " + StringUtils.join(invalidEnvNames, ", "));
    }
  }
}
