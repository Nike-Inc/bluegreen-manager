package bluegreen.manager.jobs;

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

import bluegreen.manager.model.domain.JobHistory;
import static bluegreen.manager.substituter.SubstitutionKeys.STOP_SERVICES;
import bluegreen.manager.tasks.ForgetEnvironmentTask;
import bluegreen.manager.tasks.LocalShellTask;
import bluegreen.manager.tasks.RdsInstanceDeleteTask;
import bluegreen.manager.tasks.ShellConfig;
import bluegreen.manager.tasks.Task;

/**
 * Tears down the deletion target environment, and the test database used by the former stage env.
 */
@Lazy
@Component
public class TeardownJob extends TaskSequenceJob
{
  @Autowired
  @Qualifier("shutdownApplications")
  private ShellConfig shutdownApplicationsConfig;

  @Autowired
  @Qualifier("deleteEnv")
  private ShellConfig deleteEnvConfig;

  private String deleteEnvName;
  private List<String> stopServices;

  public TeardownJob(String commandLine, boolean noop, boolean force,
                     JobHistory oldJobHistory, String deleteEnvName,
                     List<String> stopServices)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.deleteEnvName = deleteEnvName;
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
    tasks.add(applicationContext.getBean(LocalShellTask.class).assign(position++, deleteEnvName, deleteEnvConfig));
    tasks.add(applicationContext.getBean(RdsInstanceDeleteTask.class).assign(position++, deleteEnvName));
    tasks.add(applicationContext.getBean(ForgetEnvironmentTask.class).assign(position++, deleteEnvName));
    this.tasks = tasks;
  }

  private void defineSubstitutionsForShutdownApplications()
  {
    Map<String, String> substitutions = new TreeMap<String, String>();
    substitutions.put(STOP_SERVICES, StringUtils.join(stopServices, ","));
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
