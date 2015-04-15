package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.SshVmCreateTask;
import com.nike.tools.bgm.tasks.Task;

/**
 * Deploys to the stage env a copy of the pkgs that are on the live env except for explicitly specified packages that
 * must be deployed to stage.  Maps live logical databases to new stage physical db instances.
 */
@Lazy
@Component
public class StagingDeployJob extends TaskSequenceJob
{
  private String liveEnv;
  private String stageEnv;
  private Map<String, String> dbMap;
  private List<String> pkgnames;

  public StagingDeployJob(String commandLine, boolean noop, boolean force,
                          JobHistory oldJobHistory, String liveEnv, String stageEnv,
                          Map<String, String> dbMap, List<String> pkgnames)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.liveEnv = liveEnv;
    this.stageEnv = stageEnv;
    this.dbMap = dbMap;
    this.pkgnames = pkgnames;
  }

  /**
   * Instantiates the sequence of tasks for the staging deploy job.
   * <p/>
   * Is PostConstruct to have access to applicationContext.
   */
  @PostConstruct
  private void instantiateTasks()
  {
    int position = 1;
    List<Task> tasks = new ArrayList<Task>();
    //tasks.add(applicationContext.getBean(FreezeTask.class).initTransition(position++, liveEnv));
    //tasks.add(applicationContext.getBean(RDSSnapshotRestoreTask.class).init(position++, liveEnv, stageEnv, dbMap));
    //tasks.add(applicationContext.getBean(ThawTask.class).initTransition(position++, liveEnv));
    //tasks.add(new RegisterStageTask(stageEnv));
    tasks.add(applicationContext.getBean(SshVmCreateTask.class).init(position++, stageEnv));
    //tasks.add(new EnvXmlMergeTask(liveEnv, stageEnv)); //PT-2018
    //tasks.add(new DeployPackagesTask(liveEnv, stageEnv, pkgnames)); //PT-2019
    this.tasks = tasks;
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
