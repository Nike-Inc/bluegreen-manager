package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.FreezeTask;
import com.nike.tools.bgm.tasks.Task;

/**
 * Deploys to the stage env a copy of the pkgs that are on the live env except for explicitly specified packages that
 * must be deployed to stage.
 */
@Lazy
@Component
public class StagingDeployJob extends TaskSequenceJob
{
  private String liveEnv;
  private String stageEnv;
  private List<String> pkgnames;

  public StagingDeployJob(String commandLine, boolean noop, boolean force,
                          JobHistory oldJobHistory, String liveEnv, String stageEnv, List<String> pkgnames)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.liveEnv = liveEnv;
    this.stageEnv = stageEnv;
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
    tasks.add(new FreezeTask().init(position++, liveEnv)); //PT-2014
    //tasks.add(new RDSSnapshotRestoreTask(liveEnv, stageEnv)); //PT-2015
    //tasks.add(new ThawTask(liveEnv)); //PT-2014
    //tasks.add(new RegisterStageTask(stageEnv));
    //tasks.add(new VmtoolCreateTask(stageEnv)); //PT-2017
    //tasks.add(new EnvXmlMergeTask(liveEnv, stageEnv)); //PT-2018
    //tasks.add(new StagingDeployTask(liveEnv, stageEnv, pkgnames)); //PT-2019
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
