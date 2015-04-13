package com.nike.tools.bgm.jobs;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.JobStatus;
import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.tasks.Task;
import com.nike.tools.bgm.tasks.TaskRun;
import com.nike.tools.bgm.tasks.TaskRunProcessor;
import com.nike.tools.bgm.utils.NowFactory;

/**
 * Knows how to execute a sequence of tasks.  If there is a relevant recent prior job, we will not repeat its
 * successfully completed steps.
 */
public abstract class TaskSequenceJob implements Job
{
  private static Logger LOGGER = LoggerFactory.getLogger(TaskSequenceJob.class);

  @Autowired
  protected ApplicationContext applicationContext;

  @Autowired
  private NowFactory nowFactory;

  @Autowired
  private JobHistoryTx jobHistoryTx;

  @Autowired
  private TaskRunProcessor taskRunProcessor;

  /**
   * The sequence of tasks.  Initialized by PostConstruct method in derived class.
   */
  protected List<Task> tasks;

  /**
   * The original command-line, for reference.
   */
  private String commandLine;

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

  protected TaskSequenceJob(String commandLine, boolean noop, boolean force, JobHistory oldJobHistory)
  {
    this.commandLine = commandLine;
    this.noop = noop;
    this.force = force;
    this.oldJobHistory = oldJobHistory;
  }

  /**
   * Writes new job history and executes the tasks of the job.  Returns silently if success, throws if error.
   * <p/>
   * Concludes with a summary.
   */
  @Override
  public void process()
  {
    if (tasks == null || tasks.size() == 0)
    {
      throw new IllegalStateException("No tasks");
    }
    Date jobStartTime = nowFactory.now();
    openJobHistory(jobStartTime);
    JobStatus jobStatus = null;
    try
    {
      jobStatus = processTasks();
    }
    finally
    {
      if (jobStatus == null)
      {
        jobStatus = JobStatus.ERROR;
      }
      closeJobHistory(newJobHistory, jobStatus);
      logSummaryOfJobAndHistory();
    }
  }

  /**
   * Executes the tasks of the job.  Returns silently if success, throws if error.
   */
  private JobStatus processTasks()
  {
    for (int idx = 0; idx < tasks.size(); ++idx)
    {
      Task task = tasks.get(idx);
      if (idx + 1 != task.getPosition())
      {
        throw new IllegalStateException("Invalid task position: " + task.getPosition() + ", expected " + (idx + 1));
      }
      LOGGER.info("TASK #" + task.getPosition() + " of " + tasks.size() + " BEGIN: " + task.getName());
      TaskRun taskRun = new TaskRun(task, noop, force, newJobHistory, oldJobHistory);
      TaskStatus taskStatus = taskRunProcessor.attemptTask(taskRun);
      LOGGER.info("TASK #" + task.getPosition() + " of " + tasks.size() + " END: " + task.getName() + " " + taskStatus);
    }
    return JobStatus.DONE;
  }

  /**
   * Calls to persist a new JobHistory in PROCESSING state.
   */
  private void openJobHistory(Date jobStartTime)
  {
    if (!noop)
    {
      newJobHistory = jobHistoryTx.newJobHistoryProcessing(this, jobStartTime);
    }
  }

  /**
   * Calls to persist a closed JobHistory, which means setting endTime and final status.
   */
  private void closeJobHistory(JobHistory newJobHistory, JobStatus jobStatus)
  {
    if (!noop)
    {
      jobHistoryTx.closeJobHistory(newJobHistory, jobStatus);
    }
  }

  /**
   * Logs a summary of the job parameters and task results.
   */
  private void logSummaryOfJobAndHistory()
  {
    if (!noop)
    {
      String summary = summarizeJobAndHistory();
      LOGGER.info("Summary of job results:\n" + summary);
    }
  }

  /**
   * Produces a loggable string that summarizes the job parameters and task results.
   */
  private String summarizeJobAndHistory()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("============================ JOB SUMMARY =============================\n");
    sb.append("\n");
    sb.append("CommandLine: " + commandLine + "\n");
    sb.append("\n");
    sb.append("JobName:   " + getName() + "\n");
    sb.append("Noop:      " + noop + "\n");
    sb.append("Force:     " + force + "\n");
    sb.append("StartTime: " + newJobHistory.getStartTime() + "\n");
    sb.append("EndTime:   " + newJobHistory.getEndTime() + "\n");
    sb.append("JobStatus: " + newJobHistory.getStatus() + "\n");
    sb.append("\n");
    sb.append("Tasks Attempted:\n");
    final int numAttempted = newJobHistory.getTaskHistories() == null ? 0 : newJobHistory.getTaskHistories().size();
    if (numAttempted == 0)
    {
      sb.append("(none)\n");
    }
    else
    {
      for (int idx = 0; idx < numAttempted; ++idx)
      {
        TaskHistory taskHistory = newJobHistory.getTaskHistories().get(idx);
        sb.append("(" + (idx + 1) + ") " + taskHistory.getTaskName() + ": " + taskHistory.getStatus() + "\n");
      }
    }
    sb.append("\n");
    if (numAttempted < tasks.size())
    {
      sb.append("Tasks Not Attempted:\n");
      for (int idx = numAttempted; idx < tasks.size(); ++idx)
      {
        Task task = tasks.get(idx);
        sb.append("(" + (idx + 1) + ") " + task.getName() + "\n");
      }
      sb.append("\n");
    }
    sb.append("======================================================================\n");
    sb.append("\n");
    return sb.toString();
  }

  @Override
  public String getName()
  {
    return getClass().getSimpleName();
  }

  @Override
  public String getCommandLine()
  {
    return commandLine;
  }
}
