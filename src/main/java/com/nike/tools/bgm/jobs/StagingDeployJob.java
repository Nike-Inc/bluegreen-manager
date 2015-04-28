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
import com.nike.tools.bgm.tasks.FreezeTask;
import com.nike.tools.bgm.tasks.LocalShellConfig;
import com.nike.tools.bgm.tasks.LocalShellTask;
import com.nike.tools.bgm.tasks.RDSSnapshotRestoreTask;
import com.nike.tools.bgm.tasks.RegisterApplicationTask;
import com.nike.tools.bgm.tasks.SmokeTestTask;
import com.nike.tools.bgm.tasks.SshVmCreateTask;
import com.nike.tools.bgm.tasks.Task;
import com.nike.tools.bgm.tasks.ThawTask;

/**
 * Deploys to the stage env a copy of the pkgs that are on the live env except for explicitly specified packages that
 * must be deployed to stage.  Maps live logical databases to new stage physical db instances.
 */
@Lazy
@Component
public class StagingDeployJob extends TaskSequenceJob
{
  /**
   * Variable to be substituted with a comma-separated list of package names.
   */
  private static final String CMDVAR_PACKAGES = "%{packages}";

  @Autowired
  @Qualifier("createStageEnv")
  private LocalShellConfig createStageEnvConfig;

  @Autowired
  @Qualifier("deployPackages")
  private LocalShellConfig deployPackagesConfig;

  private String liveEnv;
  private String stageEnv;
  private Map<String, String> dbMap;
  private List<String> packages;

  public StagingDeployJob(String commandLine, boolean noop, boolean force,
                          JobHistory oldJobHistory, String liveEnv, String stageEnv,
                          Map<String, String> dbMap, List<String> packages)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.liveEnv = liveEnv;
    this.stageEnv = stageEnv;
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
    tasks.add(applicationContext.getBean(FreezeTask.class).initTransition(position++, liveEnv));
    tasks.add(applicationContext.getBean(RDSSnapshotRestoreTask.class).init(position++, liveEnv, stageEnv, dbMap));
    tasks.add(applicationContext.getBean(ThawTask.class).initTransition(position++, liveEnv));
    tasks.add(applicationContext.getBean(SshVmCreateTask.class).init(position++, stageEnv));
    tasks.add(applicationContext.getBean(LocalShellTask.class).init(position++, liveEnv, stageEnv, createStageEnvConfig));
    tasks.add(applicationContext.getBean(LocalShellTask.class).init(position++, liveEnv, stageEnv, deployPackagesConfig));
    tasks.add(applicationContext.getBean(RegisterApplicationTask.class).init(position++, liveEnv, stageEnv));
    tasks.add(applicationContext.getBean(SmokeTestTask.class).init(position++, stageEnv));
    this.tasks = tasks;
  }

  private void defineSubstitutionsForDeployPackages()
  {
    Map<String, String> substitutions = new TreeMap<String, String>();
    substitutions.put(CMDVAR_PACKAGES, StringUtils.join(packages, ","));
    deployPackagesConfig.setExtraSubstitutions(substitutions);
  }

  @Override
  public String getEnv1()
  {
    return liveEnv;
  }

  @Override
  public String getEnv2()
  {
    return stageEnv;
  }

}
