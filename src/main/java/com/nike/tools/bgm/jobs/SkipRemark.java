package com.nike.tools.bgm.jobs;

/**
 * Skip flag is true if the job should skip a given task, and "remark" is the reason why (suitable for logging).
 */
public class SkipRemark
{
  private boolean skip;
  private String remark;

  SkipRemark(boolean skip, String remark)
  {
    this.skip = skip;
    this.remark = remark;
  }

  public String getRemark()
  {
    return remark;
  }

  public boolean isSkip()
  {
    return skip;
  }

}
