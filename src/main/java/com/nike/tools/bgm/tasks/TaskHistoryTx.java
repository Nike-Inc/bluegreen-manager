package com.nike.tools.bgm.tasks;

import java.sql.Timestamp;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.dao.TaskHistoryDAO;
import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.NowFactory;

/**
 * Serves transactional db queries related to Task and TaskHistory.  New TaskHistory bubbles up to the parent job.
 */
@Transactional
@Component
public class TaskHistoryTx
{
  @Autowired
  private NowFactory nowFactory;

  @Autowired
  private TaskHistoryDAO taskHistoryDAO;

  /**
   * Makes a new in-progress TaskHistory, linked to a parent jobHistory, and persists it.
   * Returns the new TaskHistory.
   */
  public TaskHistory newTaskHistoryProcessing(Task task, JobHistory jobHistory)
  {
    if (task == null || jobHistory == null)
    {
      throw new IllegalArgumentException();
    }
    TaskHistory newTaskHistory = new TaskHistory();
    newTaskHistory.setJobHistory(jobHistory);
    newTaskHistory.setStartTime(new Timestamp(nowFactory.now().getTime()));
    newTaskHistory.setPosition(task.getPosition());
    newTaskHistory.setTaskName(task.getName());
    newTaskHistory.setStatus(TaskStatus.PROCESSING);
    jobHistory.addTaskHistory(newTaskHistory);
    taskHistoryDAO.persist(newTaskHistory);
    return newTaskHistory;
  }

  /**
   * Makes a new skip TaskHistory, linked to a detached parent jobHistory, and persists it.
   */
  public TaskHistory newTaskHistorySkipped(Task task, JobHistory jobHistory)
  {
    if (task == null || jobHistory == null)
    {
      throw new IllegalArgumentException();
    }
    TaskHistory newTaskHistory = new TaskHistory();
    newTaskHistory.setJobHistory(jobHistory);
    newTaskHistory.setStartTime(new Timestamp(nowFactory.now().getTime()));
    newTaskHistory.setEndTime(new Timestamp(nowFactory.now().getTime()));
    newTaskHistory.setPosition(task.getPosition());
    newTaskHistory.setTaskName(task.getName());
    newTaskHistory.setStatus(TaskStatus.SKIPPED);
    jobHistory.addTaskHistory(newTaskHistory);
    taskHistoryDAO.persist(newTaskHistory);
    return newTaskHistory;
  }

  /**
   * Closes a detached TaskHistory with an endTime and new status, then merges to the persistence context.
   */
  public void closeTaskHistory(TaskHistory taskHistory, TaskStatus taskStatus)
  {
    if (taskHistory.getId() == 0)
    {
      throw new IllegalArgumentException("Expected detached taskHistory but received new: " + taskHistory);
    }
    taskHistory.setEndTime(new Timestamp(nowFactory.now().getTime()));
    taskHistory.setStatus(taskStatus);
    taskHistoryDAO.merge(taskHistory);
  }
}
