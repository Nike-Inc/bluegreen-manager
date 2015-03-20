package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.Task;

/**
 * Swaps the liveness of the old and new envs.  Old becomes live.
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
    //TODO - fill this in
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
