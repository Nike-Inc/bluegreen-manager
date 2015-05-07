package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
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
import com.nike.tools.bgm.model.tx.TaskHistoryTx;

/**
 * Processes tasks, writes their task history, and considers the context of prior tasks in old job history.
 */
@Lazy
@Component
public class TaskRunProcessor
{
  private static Logger LOGGER = LoggerFactory.getLogger(TaskRunProcessor.class);

  @Autowired
  private SkipRemarkHelper skipRemarkHelper;

  @Autowired
  private TaskHistoryTx taskHistoryTx;

  /**
   * Attempts to process the task, according to noop/force settings and prior task history.
   * Persists new task history with the result.
   */
  public TaskStatus attemptTask(TaskRun taskRun)
  {
    boolean skip = chooseToSkipOrForce(taskRun);
    if (skip)
    {
      return skipTaskHistory(taskRun);
    }
    else
    {
      return openProcessCloseTask(taskRun);
    }
  }

  /**
   * Opens TaskHistory, processes the task, then closes TaskHistory.
   * <p/>
   * Exceptions thrown here propagate up and cause the job to end with an error.
   */
  TaskStatus openProcessCloseTask(TaskRun taskRun)
  {
    TaskStatus taskStatus = null;
    TaskHistory taskHistory = openTaskHistory(taskRun);
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      taskStatus = taskRun.getTask().process(taskRun.isNoop());
    }
    finally
    {
      LOGGER.debug("Task " + taskRun.getTask().getName() + " done ... time elapsed: " + stopWatch.toString());
      if (taskStatus == null)
      {
        taskStatus = TaskStatus.ERROR;
      }
      closeTaskHistory(taskRun.isNoop(), taskHistory, taskStatus);
    }
    return taskStatus;
  }

  /**
   * Calls to persist a new TaskHistory in PROCESSING state.
   */
  private TaskHistory openTaskHistory(TaskRun taskRun)
  {
    if (taskRun.isNoop())
    {
      return null;
    }
    else
    {
      return taskHistoryTx.newTaskHistoryProcessing(
          taskRun.getTask(), taskRun.getNewJobHistory());
    }
  }

  /**
   * Calls to persist a closed TaskHistory, which means setting endTime and final status.
   */
  private void closeTaskHistory(boolean noop, TaskHistory taskHistory, TaskStatus taskStatus)
  {
    if (!noop)
    {
      taskHistoryTx.closeTaskHistory(taskHistory, taskStatus);
    }
  }

  /**
   * In the SKIP case, calls to persist a new TaskHistory with an endTime and skip status.
   */
  TaskStatus skipTaskHistory(TaskRun taskRun)
  {
    if (!taskRun.isNoop())
    {
      taskHistoryTx.newTaskHistorySkipped(
          taskRun.getTask(), taskRun.getNewJobHistory());
    }
    return TaskStatus.SKIPPED;
  }

  /**
   * Returns true if prior task history indicates that we should skip this task.
   * Force flag can override it to false.
   */
  boolean chooseToSkipOrForce(TaskRun taskRun)
  {
    TaskHistory priorTaskHistory = findPriorTaskHistory(taskRun.getTask(), taskRun.getOldJobHistory());
    boolean skip = false;
    if (priorTaskHistory != null)
    {
      TaskStatus priorStatus = priorTaskHistory.getStatus();
      SkipRemark skipRemark = skipRemarkHelper.make(priorStatus, taskRun.isForce());
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
  TaskHistory findPriorTaskHistory(Task task, JobHistory oldJobHistory)
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
