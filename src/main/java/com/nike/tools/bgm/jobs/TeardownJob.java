package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.Task;

/**
 * Tears down the old environment.
 */
@Lazy
@Component
public class TeardownJob extends TaskSequenceJob
{
  private String oldEnv;

  public TeardownJob(String commandLine, boolean noop, boolean force,
                     JobHistory oldJobHistory, String oldEnv)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.oldEnv = oldEnv;
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
    return oldEnv;
  }

  @Override
  public String getEnv2()
  {
    return null;
  }
}
