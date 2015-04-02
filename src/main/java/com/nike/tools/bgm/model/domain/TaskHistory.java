package com.nike.tools.bgm.model.domain;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.nike.tools.bgm.utils.HashUtil;

/**
 * The history of one task.  Might still be running now, might be in the past.
 * <p/>
 * This task has an ordinal position in the parent job's sequence of tasks, starting at position 1.
 */
@Entity
@Table(name = TaskHistory.TABLE_NAME)
public class TaskHistory
{
  public static final String TABLE_NAME = "TASK_HISTORY";
  public static final String COLUMN_ID = "TASKHIST_ID";
  public static final String COLUMN_FK_JOBHIST_ID = "FK_JOBHIST_ID";
  public static final String COLUMN_START_TIME = "TASKHIST_START_TIME";
  public static final String COLUMN_END_TIME = "TASKHIST_END_TIME";
  public static final String COLUMN_POSITION = "TASKHIST_POSITION";
  public static final String COLUMN_NAME = "TASKHIST_NAME";
  public static final String COLUMN_STATUS = "TASKHIST_STATUS";
  public static final int LENGTH_NAME = 64;
  public static final int LENGTH_STATUS = 20;
  public static final String FIELD_JOB_HISTORY = "jobHistory";
  public static final int CONSTRAINT_MAX_POSITION = 100;

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long id;

  @ManyToOne
  @JoinColumn(name = COLUMN_FK_JOBHIST_ID, nullable = false)
  private JobHistory jobHistory; //FIELD_JOB_HISTORY

  @Column(name = COLUMN_START_TIME, nullable = false)
  private Timestamp startTime;

  @Column(name = COLUMN_END_TIME)
  private Timestamp endTime;

  @Min(1)
  @Max(CONSTRAINT_MAX_POSITION)
  @Column(name = COLUMN_POSITION, nullable = false)
  private int position;

  @Column(name = COLUMN_NAME, nullable = false, length = LENGTH_NAME)
  private String taskName;

  @Enumerated(EnumType.STRING)
  @Column(name = COLUMN_STATUS, nullable = false, length = LENGTH_STATUS)
  private TaskStatus status;

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof TaskHistory)
    {
      TaskHistory other = (TaskHistory) obj;
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
    sb.append("TaskHistory[");
    sb.append("id: ");
    sb.append(id);
    sb.append(", startTime: ");
    sb.append(startTime);
    sb.append(", endTime: ");
    sb.append(endTime);
    sb.append(", position: ");
    sb.append(position);
    sb.append(", taskName: ");
    sb.append(taskName);
    sb.append(", status: ");
    sb.append(status);
    sb.append("]");
    return sb.toString();
  }

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public JobHistory getJobHistory()
  {
    return jobHistory;
  }

  public void setJobHistory(JobHistory jobHistory)
  {
    this.jobHistory = jobHistory;
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

  public int getPosition()
  {
    return position;
  }

  public void setPosition(int position)
  {
    this.position = position;
  }

  public String getTaskName()
  {
    return taskName;
  }

  public void setTaskName(String taskName)
  {
    this.taskName = taskName;
  }

  public TaskStatus getStatus()
  {
    return status;
  }

  public void setStatus(TaskStatus status)
  {
    this.status = status;
  }
}
