package bluegreen.manager.tasks;

import java.util.ArrayList;
import java.util.List;

import bluegreen.manager.model.domain.JobHistory;
import bluegreen.manager.model.domain.TaskHistory;
import bluegreen.manager.model.domain.TaskStatus;
import static bluegreen.manager.utils.TimeFakery.START_TIMESTAMP;

/**
 * Test helper which makes a fake task.
 */
public class TaskFakery
{
  /**
   * Makes a list of fake TaskHistory, using the statuses in the taskStatusList.
   * <p/>
   * For old history, when building it all up at once.
   */
  public List<TaskHistory> makeFakeTaskHistories(TaskStatus[] taskStatusList, JobHistory jobHistory)
  {
    List<TaskHistory> taskHistories = new ArrayList<TaskHistory>();
    if (taskStatusList != null)
    {
      for (int idx = 0; idx < taskStatusList.length; ++idx)
      {
        taskHistories.add(makeFakeTaskHistory(idx, taskStatusList[idx], jobHistory));
      }
    }
    return taskHistories;
  }

  /**
   * Adds a single fake TaskHistory to the given jobHistory.
   * <p/>
   * For new/current history, when simulating task processing.
   */
  public void addOneFakeTaskHistory(int idx, TaskStatus taskStatus, JobHistory jobHistory)
  {
    jobHistory.getTaskHistories().add(makeFakeTaskHistory(idx, taskStatus, jobHistory));
  }

  /**
   * Makes a fake task name.  Converts 0-based idx to 1-based position.
   */
  public String makeFakeTaskName(int idx)
  {
    return "Task" + (idx + 1);
  }

  /**
   * Makes a fake TaskHistory named "Task{idx+1}" whose status is as given.
   */
  public TaskHistory makeFakeTaskHistory(int idx, TaskStatus taskStatus, JobHistory jobHistory)
  {
    TaskHistory taskHistory = new TaskHistory();
    taskHistory.setJobHistory(jobHistory);
    taskHistory.setPosition(idx + 1);
    taskHistory.setTaskName(makeFakeTaskName(idx));
    taskHistory.setStartTime(START_TIMESTAMP);
    taskHistory.setEndTime(START_TIMESTAMP);
    taskHistory.setStatus(taskStatus);
    return taskHistory;
  }

  /**
   * Makes a fake task with a name, a position, and a do-nothing process method.
   */
  public Task makeFakeTask(final int idx)
  {
    return new Task()
    {

      @Override
      public TaskStatus process(boolean noop)
      {
        return null;
      }

      @Override
      public int getPosition()
      {
        return idx + 1;
      }

      @Override
      public String getName()
      {
        return makeFakeTaskName(idx);
      }
    };
  }

}
