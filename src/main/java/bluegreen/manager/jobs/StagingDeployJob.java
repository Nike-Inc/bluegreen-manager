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
import static bluegreen.manager.substituter.SubstitutionKeys.PACKAGES;
import bluegreen.manager.tasks.EnvironmentBuildTask;
import bluegreen.manager.tasks.FreezeTask;
import bluegreen.manager.tasks.LocalShellTask;
import bluegreen.manager.tasks.RdsSnapshotRestoreTask;
import bluegreen.manager.tasks.RegisterApplicationTask;
import bluegreen.manager.tasks.ShellConfig;
import bluegreen.manager.tasks.SmokeTestTask;
import bluegreen.manager.tasks.Task;
import bluegreen.manager.tasks.ThawTask;

/**
 * Deploys to the stage env a copy of the pkgs that are on the live env except for explicitly specified packages that
 * must be deployed to stage.  Maps live logical databases to new stage physical db instances.
 */
@Lazy
@Component
public class StagingDeployJob extends TaskSequenceJob
{
  @Autowired
  @Qualifier("buildStageEnv")
  private ShellConfig buildStageEnvConfig;

  @Autowired
  @Qualifier("deployPackages")
  private ShellConfig deployPackagesConfig;

  private String liveEnvName;
  private String stageEnvName;
  private Map<String, String> dbMap;
  private List<String> packages;

  public StagingDeployJob(String commandLine, boolean noop, boolean force,
                          JobHistory oldJobHistory, String liveEnvName, String stageEnvName,
                          Map<String, String> dbMap, List<String> packages)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.liveEnvName = liveEnvName;
    this.stageEnvName = stageEnvName;
    this.dbMap = dbMap;
    this.packages = packages;
  }

  /**
   * Instantiates the sequence of tasks for the staging deploy job.
   * <p/>
   * Is PostConstruct to have access to applicationContext.
   */
  @PostConstruct
  private void instantiateTasks()
  {
    defineSubstitutionsForDeployPackages();
    int position = 1;
    List<Task> tasks = new ArrayList<Task>();
    tasks.add(applicationContext.getBean(FreezeTask.class).assignTransition(position++, liveEnvName));
    tasks.add(applicationContext.getBean(RdsSnapshotRestoreTask.class).assign(position++, liveEnvName, stageEnvName, dbMap));
    tasks.add(applicationContext.getBean(ThawTask.class).assignTransition(position++, liveEnvName));
    tasks.add(applicationContext.getBean(EnvironmentBuildTask.class).assign(position++, liveEnvName, stageEnvName, buildStageEnvConfig, false));
    tasks.add(applicationContext.getBean(LocalShellTask.class).assign(position++, liveEnvName, stageEnvName, deployPackagesConfig, true));
    tasks.add(applicationContext.getBean(RegisterApplicationTask.class).assign(position++, liveEnvName, stageEnvName));
    tasks.add(applicationContext.getBean(SmokeTestTask.class).assign(position++, stageEnvName));
    this.tasks = tasks;
  }

  private void defineSubstitutionsForDeployPackages()
  {
    Map<String, String> substitutions = new TreeMap<String, String>();
    substitutions.put(PACKAGES, StringUtils.join(packages, ","));
    deployPackagesConfig.setExtraSubstitutions(substitutions);
  }

  @Override
  public String getEnv1()
  {
    return liveEnvName;
  }

  @Override
  public String getEnv2()
  {
    return stageEnvName;
  }

}
