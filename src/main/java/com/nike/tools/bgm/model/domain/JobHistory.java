package com.nike.tools.bgm.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.nike.tools.bgm.utils.HashUtil;

/**
 * The history of one job.  Might still be running now, might be in the past.
 * <p/>
 * A job involves one or two envs, parsed from the command-line.
 */
@Entity
@Table(name = JobHistory.TABLE_NAME)
public class JobHistory
{
  public static final String TABLE_NAME = "JOB_HISTORY";
  public static final String COLUMN_ID = "JOBHIST_ID";
  public static final String COLUMN_START_TIME = "JOBHIST_START_TIME";
  public static final String COLUMN_END_TIME = "JOBHIST_END_TIME";
  public static final String COLUMN_NAME = "JOBHIST_NAME";
  public static final String COLUMN_ENV1 = "JOBHIST_ENV1";
  public static final String COLUMN_ENV2 = "JOBHIST_ENV2";
  public static final String COLUMN_CMDLINE = "JOBHIST_CMDLINE";
  public static final String COLUMN_STATUS = "JOBHIST_STATUS";
  public static final int LENGTH_NAME = 64;
  public static final int LENGTH_ENV1 = Environment.LENGTH_ENV_NAME;
  public static final int LENGTH_ENV2 = Environment.LENGTH_ENV_NAME;
  public static final int LENGTH_STATUS = 20;
  public static final int LENGTH_CMDLINE = 1024;

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long id;

  @Column(name = COLUMN_START_TIME, nullable = false)
  private Timestamp startTime;

  @Column(name = COLUMN_END_TIME)
  private Timestamp endTime;

  @Column(name = COLUMN_NAME, nullable = false, length = LENGTH_NAME)
  private String jobName;

  @Column(name = COLUMN_ENV1, nullable = false, length = LENGTH_ENV1)
  private String env1;

  @Column(name = COLUMN_ENV2, length = LENGTH_ENV2)
  private String env2;

  @Column(name = COLUMN_CMDLINE, nullable = false, length = LENGTH_CMDLINE)
  private String commandLine;

  @Enumerated(EnumType.STRING)
  @Column(name = COLUMN_STATUS, nullable = false, length = LENGTH_STATUS)
  private JobStatus status;

  @OneToMany(mappedBy = TaskHistory.FIELD_JOB_HISTORY, cascade = CascadeType.ALL)
  private List<TaskHistory> taskHistories;

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof Environment)
    {
      JobHistory other = (JobHistory) obj;
      return id == other.id;
    }
    return false;
  }

  /**
   * Hashcode based solely on database identity.
   */
  @Override
  public int hashCode()
  {
    return HashUtil.hashId(id);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("JobHistory[");
    sb.append("id: ");
    sb.append(id);
    sb.append(", startTime: ");
    sb.append(startTime);
    sb.append(", endTime: ");
    sb.append(endTime);
    sb.append(", jobName: ");
    sb.append(jobName);
    sb.append(", env1: ");
    sb.append(env1);
    sb.append(", env2: ");
    sb.append(env2);
    sb.append(", status: ");
    sb.append(status);
    sb.append(", commandLine: ");
    sb.append(commandLine);
    sb.append("]");
    return sb.toString();
  }

  /**
   * Adds the taskHistory to the job.  Initializes the job's taskHistory list if needed.
   * <p/>
   * Does not check whether taskHistory is already in the list.
   */
  public void addTaskHistory(TaskHistory taskHistory)
  {
    if (taskHistories == null)
    {
      taskHistories = new ArrayList<TaskHistory>();
    }
    taskHistories.add(taskHistory);
  }

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public Timestamp getStartTime()
  {
    return startTime;
  }

  public void setStartTime(Timestamp startTime)
  {
    this.startTime = startTime;
  }

  public Timestamp getEndTime()
  {
    return endTime;
  }

  public void setEndTime(Timestamp endTime)
  {
    this.endTime = endTime;
  }

  public String getJobName()
  {
    return jobName;
  }

  public void setJobName(String jobName)
  {
    this.jobName = jobName;
  }

  public String getEnv1()
  {
    return env1;
  }

  public void setEnv1(String env1)
  {
    this.env1 = env1;
  }

  public String getEnv2()
  {
    return env2;
  }

  public void setEnv2(String env2)
  {
    this.env2 = env2;
  }

  public String getCommandLine()
  {
    return commandLine;
  }

  public void setCommandLine(String commandLine)
  {
    this.commandLine = commandLine;
  }

  public JobStatus getStatus()
  {
    return status;
  }

  public void setStatus(JobStatus status)
  {
    this.status = status;
  }

  public List<TaskHistory> getTaskHistories()
  {
    return taskHistories;
  }

  public void setTaskHistories(List<TaskHistory> taskHistories)
  {
    this.taskHistories = taskHistories;
  }
}
