package com.nike.tools.bgm.jobs;

import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;

/**
 * Makes the little SkipRemark object.
 */
@Component
public class SkipRemarkHelper
{
  /**
   * Makes a skip remark based on the non-null prior status and force flag.
   */
  public SkipRemark make(TaskStatus priorStatus, boolean force)
  {
    if (priorStatus == null)
    {
      throw new IllegalArgumentException();
    }
    if (force)
    {
      return makeForced(priorStatus);
    }
    else
    {
      return makeNoForce(priorStatus);
    }
  }

  /**
   * Makes the normal skip remark, assuming no force.
   */
  private SkipRemark makeNoForce(TaskStatus priorStatus)
  {
    boolean skip = false;
    String remark = null;
    switch (priorStatus)
    {
      case SKIPPED:
        remark = "will skip it again";
        skip = true;
        break;
      case DONE:
        remark = "will skip it now";
        skip = true;
        break;
      case PROCESSING:
        remark = "must have been interrupted or timed out, will try it again now";
        break;
      case ERROR:
        remark = "will try it again now";
        break;
      default:
        throw new IllegalStateException();
    }
    return new SkipRemark(skip, remark);
  }

  /**
   * Makes the skip remark, assuming force is true.
   */
  private SkipRemark makeForced(TaskStatus priorStatus)
  {
    boolean skip = false;
    String remark = null;
    switch (priorStatus)
    {
      case SKIPPED:
        remark = "will run it now";
        break;
      case DONE:
        remark = "will run it again now";
        break;
      case PROCESSING:
        remark = "must have been interrupted or timed out, will try it again now";
        break;
      case ERROR:
        remark = "will try it again now";
        break;
      default:
        throw new IllegalStateException();
    }
    return new SkipRemark(skip, remark + " (force=true)");
  }

  /**
   * Uses the remark to summarize prior task history and what we'll do now.
   */
  public String useRemark(SkipRemark skipRemark, TaskHistory priorTaskHistory)
  {
    return "Prior task execution on " + priorTaskHistory.getStartTime() + ": " + priorTaskHistory.getStatus()
        + "" + skipRemark.getRemark();
  }

}
