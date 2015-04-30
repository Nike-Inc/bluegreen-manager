package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.DiscoveryTask;
import com.nike.tools.bgm.tasks.FreezeTask;
import com.nike.tools.bgm.tasks.LinkLiveDatabaseTask;
import com.nike.tools.bgm.tasks.Task;

/**
 * Swaps the liveness of the old and new envs.  Old becomes live.
 * <p/>
 * stagingDeploy liveEnv becomes goLive oldLiveEnv.
 * stagingDeploy stageEnv becomes goLive newLiveEnv.
 */
@Lazy
@Component
public class GoLiveJob extends TaskSequenceJob
{
  private String oldLiveEnv;
  private String newLiveEnv;

  public GoLiveJob(String commandLine, boolean noop, boolean force,
                   JobHistory oldJobHistory, String oldLiveEnv, String newLiveEnv)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.oldLiveEnv = oldLiveEnv;
    this.newLiveEnv = newLiveEnv;
  }

  /**
   * Instantiates the sequence of tasks for the go-live job.
   * <p/>
   * Is PostConstruct to have access to applicationContext.
   */
  @PostConstruct
  private void instantiateTasks()
  {
    int position = 1;
    List<Task> tasks = new ArrayList<Task>();
    tasks.add(applicationContext.getBean(FreezeTask.class).assignTransition(position++, newLiveEnv));
    tasks.add(applicationContext.getBean(FreezeTask.class).assignTransition(position++, oldLiveEnv));
    tasks.add(applicationContext.getBean(LinkLiveDatabaseTask.class).assign(position++, oldLiveEnv, newLiveEnv));
    tasks.add(applicationContext.getBean(DiscoveryTask.class).assign(position++, newLiveEnv));
    this.tasks = tasks;
  }

  @Override
  public String getEnv1()
  {
    return oldLiveEnv;
  }

  @Override
  public String getEnv2()
  {
    return newLiveEnv;
  }
}
