package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
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
  public static final String PARAMNAME_DB_MAP = "dbMap";
  public static final String PARAMNAME_PACKAGES = "packages";
  public static final String PARAMNAME_OLD_LIVE_ENV = "oldLiveEnv";
  public static final String PARAMNAME_NEW_LIVE_ENV = "newLiveEnv";
  public static final String PARAMNAME_FIXED_LB = "fixedLB";
  public static final String PARAMNAME_NOOP = "noop";
  public static final String PARAMNAME_FORCE = "force";

  private static final long MAX_AGE_RELEVANT_PRIOR_JOB = 1000L * 60L * 60L * 24L; //1 day
  static final int UNLIMITED_NUM_VALUES = -1;

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
  String makeExplanationOfValidJobs()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("BlueGreenManager argument format: <jobName> <parameters>\n");
    sb.append("\n");
    sb.append("Job '" + JOBNAME_STAGING_DEPLOY + "'\n");
    sb.append("Description: Spins up a new stage env, including a new application vm, its target application, and a test copy\n");
    sb.append("             of the live database.  Temporarily freezes the live application during database copy.\n");
    sb.append("Required Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_LIVE_ENV + " <envName>\n");
    sb.append("\t\t\tSpecify the live env so we can freeze it, replicate its db, and base stage from it.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_STAGE_ENV + " <envName>\n");
    sb.append("\t\t\tThe stage env is where we will spin up a new vm, a test db copy, and deploy packages.\n");
    sb.append("\t\t\tIt must not exist beforehand, because this job creates it.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_DB_MAP + " [ <liveLogicalName> <stagePhysicalInstName> ]+\n");
    sb.append("\t\t\tWe will copy the live logical database(s) to a stage physical database having the requested instance name.\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_PACKAGES + " <list of stage pkgs>\n");
    sb.append("\t\t\tList of packages to deploy to stage, which are DIFFERENT from what is on the live env.\n");
    sb.append("\t\t\tUse full package names as they will be recognized by your package repository.\n");
    sb.append("\n");
    sb.append("Job '" + JOBNAME_GO_LIVE + "'\n");
    sb.append("Description: Reassigns liveness from the old env to the new env.  When done, the old env is frozen and removed\n");
    sb.append("             from the live load balancer's vm pool.\n");
    sb.append("Required Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_OLD_LIVE_ENV + " <envName>\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_NEW_LIVE_ENV + " <envName>\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_FIXED_LB + " <loadBalancerName>\n");
    sb.append("\t\t\tName of a fixed live load-balancer currently hosting the old live application.  We keep the LB fixed in place,\n");
    sb.append("\t\t\tregister the new live application vm with this LB, and deregister the old live application vm.\n");
    sb.append("\n");
    sb.append("Job '" + JOBNAME_TEARDOWN + "'\n");
    sb.append("Description: Spins down and destroys the old live env, and the test database left in the new live env.\n");
    sb.append("Required Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_OLD_LIVE_ENV + " <envName>\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_NEW_LIVE_ENV + " <envName>\n");
    sb.append("\n");
    sb.append("Common Optional Parameters:\n");
    sb.append("\t" + ArgumentParser.DOUBLE_HYPHEN + PARAMNAME_NOOP + "\n");
    sb.append("\t\t\tNo-op means print out what this job WOULD do, without taking any action that would leave side effects.\n");
    sb.append("\t\t\tWe will make read-only queries to env services to gather useful information.\n");
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
   * <p/>
   * We don't verify whether stageEnv exists because the logic is beyond what JobFactory should calculate.  If we are
   * running the job from the first task, then it must not exist; if we are skipping past the last recent task that
   * created it then it must exist.  We leave these assertions to the tasks.
   */
  private Job makeStagingDeployJob(List<List<String>> parameters, String commandLine)
  {
    Map<String, String> dbMap = listToMap(getParameterValues(PARAMNAME_DB_MAP, parameters), PARAMNAME_DB_MAP);
    List<String> packages = getParameterValues(PARAMNAME_PACKAGES, parameters);
    return makeGenericJob(StagingDeployJob.class, parameters, commandLine, PARAMNAME_LIVE_ENV, PARAMNAME_STAGE_ENV, false, dbMap, packages);
  }

  /**
   * Constructs a new GoLiveJob with the specified parameters.
   */
  private Job makeGoLiveJob(List<List<String>> parameters, String commandLine)
  {
    String fixedLbName = getParameter(PARAMNAME_FIXED_LB, parameters, 1).get(1);
    return makeGenericJob(GoLiveJob.class, parameters, commandLine, PARAMNAME_OLD_LIVE_ENV, PARAMNAME_NEW_LIVE_ENV, true, fixedLbName);
  }

  /**
   * Constructs a new TeardownJob with the specified parameters.
   */
  private Job makeTeardownJob(List<List<String>> parameters, String commandLine)
  {
    return makeGenericJob(TeardownJob.class, parameters, commandLine, PARAMNAME_OLD_LIVE_ENV, PARAMNAME_NEW_LIVE_ENV, true);
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
                             boolean verifyBothEnvs,
                             Object... otherArgs)
  {
    boolean noop = hasParameter(PARAMNAME_NOOP, parameters);
    boolean force = hasParameter(PARAMNAME_FORCE, parameters);
    String env1 = getParameter(env1ParamName, parameters, 1).get(1);
    String env2 = getParameter(env2ParamName, parameters, 1).get(1);
    verifyOneOrTwoEnvNames(env1, env2, verifyBothEnvs);
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
  List<String> getParameter(String paramName, List<List<String>> parameters, int expectedNumValues)
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
   * Requires that there be at least one value.
   * <p/>
   * Each parameter is a sublist of parameter parts.  (ParamName, val1, val2, ...)
   * In this case returns (val1, val2, ...)
   */
  List<String> getParameterValues(String paramName, List<List<String>> parameters)
  {
    List<String> parameter = getParameter(paramName, parameters, UNLIMITED_NUM_VALUES);
    if (parameter.size() <= 1)
    {
      throw new CmdlineException("Parameter '" + paramName + "' expects some values, found none");
    }
    return new ArrayList<String>(parameter.subList(1, parameter.size())); //i.e. skip item#0 (paramName)
  }

  /**
   * Returns true if the named parameter was specified.
   */
  boolean hasParameter(String paramName, List<List<String>> parameters)
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
   * Verifies env1, and env2 if requested to verifyBothEnvs.
   */
  private void verifyOneOrTwoEnvNames(String env1, String env2, boolean verifyBothEnvs)
  {
    if (!verifyBothEnvs)
    {
      verifyEnvNames(env1);
    }
    else
    {
      verifyEnvNames(env1, env2);
    }
  }

  /**
   * Checks that all the specified envNames exist in the environment table.  Throws exception if any don't.
   * Returns silently if they all exist.
   */
  void verifyEnvNames(String... envNames)
  {
    if (envNames == null || envNames.length == 0)
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

  /**
   * Given a list of tokens (t1, t2, t3, t4, ...), returns a map {t1=>t2, t3=>t4, ...}.
   * <p/>
   * The tokens are values of the parameter with the given paramName.
   */
  Map<String, String> listToMap(List<String> tokens, String paramName)
  {
    if (CollectionUtils.isEmpty(tokens))
    {
      throw new CmdlineException("Parameter '" + paramName + "' expects nonzero list of values");
    }
    else if (tokens.size() % 2 == 1)
    {
      throw new CmdlineException("Parameter '" + paramName + "' expects even number of values, but found " + tokens.size());
    }
    Map<String, String> map = new TreeMap<String, String>();
    for (int idx = 0; idx < tokens.size(); idx += 2)
    {
      map.put(tokens.get(idx), tokens.get(idx + 1));
    }
    return map;
  }
}
