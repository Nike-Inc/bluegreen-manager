package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.Task;

/**
 * Tears down the old environment, and the test database remaining in the new live env.
 */
@Lazy
@Component
public class TeardownJob extends TaskSequenceJob
{
  private String oldLiveEnv;
  private String newLiveEnv;

  public TeardownJob(String commandLine, boolean noop, boolean force,
                     JobHistory oldJobHistory, String oldLiveEnv, String newLiveEnv)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.oldLiveEnv = oldLiveEnv;
    this.newLiveEnv = newLiveEnv;
  }

  /**
   * Instantiates the sequence of tasks for the teardown job.
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
