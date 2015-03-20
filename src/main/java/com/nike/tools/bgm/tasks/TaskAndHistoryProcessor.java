package com.nike.tools.bgm.tasks;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.jobs.SkipRemark;
import com.nike.tools.bgm.jobs.SkipRemarkHelper;
import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.NowFactory;

/**
 * Processes tasks, writes their task history, and considers the context of prior tasks in old job history.
 */
@Lazy
@Component
public class TaskAndHistoryProcessor
{
  private static Logger LOGGER = LoggerFactory.getLogger(TaskAndHistoryProcessor.class);

  @Autowired
  NowFactory nowFactory;

  @Autowired
  private SkipRemarkHelper skipRemarkHelper;

  @Autowired
  private TaskHistoryTx taskHistoryTx;

  /**
   * If true, just display what this job WOULD do.  Makes this job harmless.
   */
  private boolean noop;

  /**
   * Forces the job to attempt all tasks, instead of skipping previously successful tasks in the last relevant run.
   */
  private boolean force;

  /**
   * Persistent record of the current job run.
   */
  private JobHistory newJobHistory;

  /**
   * The last relevant job run.  Null if any prior job runs are too old to be considered.
   */
  private JobHistory oldJobHistory;

  public TaskAndHistoryProcessor(boolean noop,
                                 boolean force,
                                 JobHistory newJobHistory,
                                 JobHistory oldJobHistory)
  {
    this.noop = noop;
    this.force = force;
    this.newJobHistory = newJobHistory;
    this.oldJobHistory = oldJobHistory;
  }

  /**
   * Attempts to process the task, according to noop/force settings and prior task history.
   * Persists new task history with the result.
   */
  public TaskStatus attemptTask(Task task)
  {
    Date taskStartTime = nowFactory.now();
    TaskStatus taskStatus = null;
    boolean skip = chooseToSkipOrForce(task);
    if (skip)
    {
      return skipTaskHistory(task, taskStartTime);
    }
    else
    {
      return openProcessCloseTask(task, taskStartTime);
    }
  }

  /**
   * Opens TaskHistory, processes the task, then closes TaskHistory.
   * <p/>
   * Exceptions thrown here propagate up and cause the job to end with an error.
   */
  private TaskStatus openProcessCloseTask(Task task, Date taskStartTime)
  {
    TaskStatus taskStatus = null;
    TaskHistory taskHistory = openTaskHistory(task, taskStartTime);
    try
    {
      taskStatus = task.process(noop);
    }
    finally
    {
      if (taskStatus == null)
      {
        taskStatus = TaskStatus.ERROR;
      }
      closeTaskHistory(taskHistory, taskStatus);
    }
    return taskStatus;
  }

  /**
   * Calls to persist a new TaskHistory in PROCESSING state.
   */
  private TaskHistory openTaskHistory(Task task, Date taskStartTime)
  {
    if (noop)
    {
      return null;
    }
    else
    {
      return taskHistoryTx.newTaskHistoryProcessing(task, taskStartTime, newJobHistory);
    }
  }

  /**
   * Calls to persist a closed TaskHistory, which means setting endTime and final status.
   */
  private void closeTaskHistory(TaskHistory taskHistory, TaskStatus taskStatus)
  {
    if (!noop)
    {
      taskHistoryTx.closeTaskHistory(taskHistory, taskStatus);
    }
  }

  /**
   * In the SKIP case, calls to persist a new TaskHistory with an endTime and skip status.
   */
  private TaskStatus skipTaskHistory(Task task, Date taskStartTime)
  {
    if (!noop)
    {
      taskHistoryTx.newTaskHistorySkipped(task, taskStartTime, newJobHistory);
    }
    return TaskStatus.SKIPPED;
  }

  /**
   * Returns true if prior task history indicates that we should skip this task.
   * Force flag can override it to false.
   */
  private boolean chooseToSkipOrForce(Task task)
  {
    TaskHistory priorTaskHistory = findPriorTaskHistory(task);
    boolean skip = false;
    if (priorTaskHistory != null)
    {
      TaskStatus priorStatus = priorTaskHistory.getStatus();
      SkipRemark skipRemark = skipRemarkHelper.make(priorStatus, force);
      LOGGER.info(skipRemarkHelper.useRemark(skipRemark, priorTaskHistory));
      skip = skipRemark.isSkip();
    }
    return skip;
  }

  /**
   * Finds prior execution of this task, if any.
   * <p/>
   * Note: Task matching is based on task class and ordinal position.  If the job definition has
   * changed then this probably will produce incorrect results and bluegreen users should force a
   * complete job run.
   */
  private TaskHistory findPriorTaskHistory(Task task)
  {
    if (oldJobHistory != null && oldJobHistory.getTaskHistories() != null)
    {
      final int position = task.getPosition();
      final String taskName = task.getName();
      for (TaskHistory taskHistory : oldJobHistory.getTaskHistories())
      {
        if (taskHistory.getPosition() == position && StringUtils.equals(taskHistory.getTaskName(), taskName))
        {
          return taskHistory;
        }
      }
    }
    return null;
  }

}
