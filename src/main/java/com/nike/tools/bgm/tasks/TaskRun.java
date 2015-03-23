package com.nike.tools.bgm.tasks;

import java.util.Date;

import com.nike.tools.bgm.model.domain.JobHistory;

/**
 * Represents a task with runtime parameters and relevant history objects.
 */
public class TaskRun
{
  /**
   * Task being considered for processing.
   */
  private Task task;

  /**
   * If true, just display what this job WOULD do.  Makes this job harmless.
   */
  private boolean noop;

  /**
   * Forces the job to attempt all tasks, instead of skipping previously successful tasks in the last relevant run.
   */
  private boolean force;

  /**
   * When this task started.
   */
  private Date startTime;

  /**
   * Persistent record of the current job run.
   */
  private JobHistory newJobHistory;

  /**
   * The last relevant job run.  Null if any prior job runs are too old to be considered.
   */
  private JobHistory oldJobHistory;

  public TaskRun(Task task,
                 boolean noop,
                 boolean force,
                 Date startTime,
                 JobHistory newJobHistory, JobHistory oldJobHistory)
  {
    this.task = task;
    this.noop = noop;
    this.force = force;
    this.startTime = startTime;
    this.newJobHistory = newJobHistory;
    this.oldJobHistory = oldJobHistory;
  }

  public Task getTask()
  {
    return task;
  }

  public boolean isNoop()
  {
    return noop;
  }

  public boolean isForce()
  {
    return force;
  }

  public Date getStartTime()
  {
    return startTime;
  }

  public JobHistory getNewJobHistory()
  {
    return newJobHistory;
  }

  public JobHistory getOldJobHistory()
  {
    return oldJobHistory;
  }
}
