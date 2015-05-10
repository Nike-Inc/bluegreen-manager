package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.LocalShellConfig;
import com.nike.tools.bgm.tasks.LocalShellTask;
import com.nike.tools.bgm.tasks.SshVmDeleteTask;
import com.nike.tools.bgm.tasks.Task;

/**
 * Tears down a target environment, and the test database used by the former stage env.
 * <p/>
 * "Commit" case: In the case of successful stagingDeploy and goLive, the deletion env is the old live env.
 * "Rollback" case: In case of failed post-stagingDeploy test, the delete env is the stage env ("new live env").
 */
@Lazy
@Component
public class TeardownJob extends TaskSequenceJob
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
  private String deleteDbPhysicalInstanceName;
  private List<String> stopServices;
  private boolean commit; //false=rollback

  public TeardownJob(String commandLine, boolean noop, boolean force,
                     JobHistory oldJobHistory, String deleteEnvName, String deleteDbPhysicalInstanceName,
                     List<String> stopServices, boolean commit)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.deleteEnvName = deleteEnvName;
    this.deleteDbPhysicalInstanceName = deleteDbPhysicalInstanceName;
    this.stopServices = stopServices;
    this.commit = commit;
  }

  /**
   * Instantiates the sequence of tasks for the teardown job.
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
    //tasks.add(applicationContext.getBean(LocalShellTask.class).assign(position++, deleteEnvName, deleteEnvConfig));
    //tasks.add(applicationContext.getBean(RdsInstanceDeleteTask.class).init(position++, deleteEnvName, deleteDbPhysicalInstanceName));
    //tasks.add(applicationContext.getBean(ForgetEnvironmentTask.class).assign(position++, deleteEnvName));
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
