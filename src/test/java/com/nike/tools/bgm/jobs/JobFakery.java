package com.nike.tools.bgm.jobs;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.tasks.TaskFakery;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIMESTAMP;

public class JobFakery
{
  public static String JOB_NAME = "theJob";
  public static String ENV_NAME1 = "firstEnv";
  public static String ENV_NAME2 = "secondEnv";
  public static String COMMAND_LINE = JOB_NAME + " --arg1=" + ENV_NAME1 + " --arg2=" + ENV_NAME2;

  private TaskFakery taskFakery;

  public JobFakery(TaskFakery taskFakery)
  {
    this.taskFakery = taskFakery;
  }

  /**
   * Makes a fake JobHistory with a list of TaskHistory having the given task statuses.
   * <p/>
   * Not setting start/endTime, env1/2, commandLine.
   */
  public JobHistory makeFakeJobHistory(TaskStatus[] taskStatusList)
  {
    JobHistory jobHistory = new JobHistory();
    jobHistory.setStartTime(START_TIMESTAMP);
    jobHistory.setJobName(JOB_NAME);
    jobHistory.setTaskHistories(taskFakery.makeFakeTaskHistories(taskStatusList, jobHistory));
    return jobHistory;
  }

  /**
   * Returns a fake job with some interesting looking String values but a do-nothing process method.
   */
  public Job makeFakeJob()
  {
    return new Job()
    {

      @Override
      public void process()
      {
        //Do nothing
      }

      @Override
      public String getName()
      {
        return JOB_NAME;
      }

      @Override
      public String getEnv1()
      {
        return ENV_NAME1;
      }

      @Override
      public String getEnv2()
      {
        return ENV_NAME2;
      }

      @Override
      public String getCommandLine()
      {
        return COMMAND_LINE;
      }
    };
  }
}
