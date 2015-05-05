package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.tasks.Task;

/**
 * Tears down a target environment, and the test database used by the former stage env.
 * <p/>
 * "commit" case: In the case of successful stagingDeploy and goLive, the deletion env is the old live env.
 * "rollback" case: In case of failed post-stagingDeploy test, the delete env is the stage env ("new live env").
 */
@Lazy
@Component
public class TeardownJob extends TaskSequenceJob
{
  private String deleteEnvName;
  private String deleteDbPhysicalInstanceName;
  private boolean commit; //false=rollback

  public TeardownJob(String commandLine, boolean noop, boolean force,
                     JobHistory oldJobHistory, String deleteEnvName,
                     String deleteDbPhysicalInstanceName, boolean commit)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.deleteEnvName = deleteEnvName;
    this.deleteDbPhysicalInstanceName = deleteDbPhysicalInstanceName;
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
    int position = 1;
    List<Task> tasks = new ArrayList<Task>();
    //TODO - fill this in
    this.tasks = tasks;
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
