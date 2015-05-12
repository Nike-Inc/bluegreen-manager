package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.ForgetEnvironmentTask;
import com.nike.tools.bgm.tasks.LocalShellConfig;
import com.nike.tools.bgm.tasks.LocalShellTask;
import com.nike.tools.bgm.tasks.RdsInstanceDeleteTask;
import com.nike.tools.bgm.tasks.SshVmDeleteTask;
import com.nike.tools.bgm.tasks.Task;

/**
 * Tears down the deletion target environment, and the test database used by the former stage env.
 * <p/>
 * This would be a one-env job except for the fact that you need the live env to make the name of the bluegreen db
 * snapshot from which the test db was originally made.  So this is a two-env job.  With rollbackStageJob the two envs
 * are different (stage env and live env); with teardownCommitJob they are the same (both the old live env).
 */
public abstract class TeardownJob extends TaskSequenceJob
{
  /**
   * Variable to be substituted with a comma-separated list of app names.
   */
  private static final String CMDVAR_STOP_SERVICES = "%{stopServices}";

  @Autowired
  @Qualifier("shutdownApplications")
  private LocalShellConfig shutdownApplicationsConfig;

  @Autowired
  @Qualifier("deleteEnv")
  private LocalShellConfig deleteEnvConfig;

  private String deleteEnvName;
  private String liveEnvName; //For READ-ONLY purposes.
  private List<String> stopServices;

  public TeardownJob(String commandLine, boolean noop, boolean force,
                     JobHistory oldJobHistory, String deleteEnvName,
                     String liveEnvName, List<String> stopServices)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.deleteEnvName = deleteEnvName;
    this.liveEnvName = liveEnvName;
    this.stopServices = stopServices;
  }

  /**
   * Instantiates the sequence of tasks for the teardown-commit job.
   * <p/>
   * Is PostConstruct to have access to applicationContext.
   */
  @PostConstruct
  private void instantiateTasks()
  {
    defineSubstitutionsForShutdownApplications();
    int position = 1;
    List<Task> tasks = new ArrayList<Task>();
    tasks.add(applicationContext.getBean(LocalShellTask.class).assign(position++, deleteEnvName, shutdownApplicationsConfig));
    tasks.add(applicationContext.getBean(SshVmDeleteTask.class).init(position++, deleteEnvName));
    tasks.add(applicationContext.getBean(LocalShellTask.class).assign(position++, deleteEnvName, deleteEnvConfig));
    tasks.add(applicationContext.getBean(RdsInstanceDeleteTask.class).assign(position++, deleteEnvName, liveEnvName));
    tasks.add(applicationContext.getBean(ForgetEnvironmentTask.class).assign(position++, deleteEnvName));
    this.tasks = tasks;
  }

  private void defineSubstitutionsForShutdownApplications()
  {
    Map<String, String> substitutions = new TreeMap<String, String>();
    substitutions.put(CMDVAR_STOP_SERVICES, StringUtils.join(stopServices, ","));
    shutdownApplicationsConfig.setExtraSubstitutions(substitutions);
  }

  @Override
  public String getEnv1()
  {
    return deleteEnvName;
  }

  @Override
  public String getEnv2()
  {
    return null;
  }
}
