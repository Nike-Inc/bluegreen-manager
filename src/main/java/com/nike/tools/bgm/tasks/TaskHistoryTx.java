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
 * Serves transactional db queries related to Task and TaskHistory.
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
   * Makes an in-progress TaskHistory (linked to a parent jobHistory) and saves it.
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
    taskHistoryDAO.save(newTaskHistory);
    return newTaskHistory;
  }

  /**
   * Makes a skip TaskHistory (linked to a parent jobHistory) and saves it.
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
    taskHistoryDAO.save(newTaskHistory);
    return newTaskHistory;
  }

  /**
   * Finds the current TaskHistory and closes it with an endTime and new status.
   */
  public void closeTaskHistory(TaskHistory taskHistory, TaskStatus taskStatus)
  {
    taskHistoryDAO.refresh(taskHistory);
    taskHistory.setEndTime(new Timestamp(nowFactory.now().getTime()));
    taskHistory.setStatus(taskStatus);
    taskHistoryDAO.save(taskHistory);
  }
}
