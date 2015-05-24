package bluegreen.manager.utils;

/**
 * Wait delays, number of waits before timeout, etc.  Applies to a Waiter.
 */
public class WaiterParameters
{
  /**
   * How long waiter should sleep after the initial check.
   */
  private long initialWaitDelayMilliseconds;

  /**
   * How long waiter should sleep after a followup check.
   */
  private long followupWaitDelayMilliseconds;

  /**
   * Set to 1 to report time elapsed on every iteration, or 10 for every 10th iteration.
   */
  private int waitReportInterval;

  /**
   * Timeout occurs after max num waits.  Number includes the initial wait.
   */
  private int maxNumWaits;

  public WaiterParameters()
  {
  }

  public WaiterParameters(long initialWaitDelayMilliseconds,
                          long followupWaitDelayMilliseconds,
                          int waitReportInterval,
                          int maxNumWaits)
  {
    this.initialWaitDelayMilliseconds = initialWaitDelayMilliseconds;
    this.followupWaitDelayMilliseconds = followupWaitDelayMilliseconds;
    this.waitReportInterval = waitReportInterval;
    this.maxNumWaits = maxNumWaits;
  }

  public long getInitialWaitDelayMilliseconds()
  {
    return initialWaitDelayMilliseconds;
  }

  public void setInitialWaitDelayMilliseconds(long initialWaitDelayMilliseconds)
  {
    this.initialWaitDelayMilliseconds = initialWaitDelayMilliseconds;
  }

  public long getFollowupWaitDelayMilliseconds()
  {
    return followupWaitDelayMilliseconds;
  }

  public void setFollowupWaitDelayMilliseconds(long followupWaitDelayMilliseconds)
  {
    this.followupWaitDelayMilliseconds = followupWaitDelayMilliseconds;
  }

  public int getWaitReportInterval()
  {
    return waitReportInterval;
  }

  public void setWaitReportInterval(int waitReportInterval)
  {
    this.waitReportInterval = waitReportInterval;
  }

  public int getMaxNumWaits()
  {
    return maxNumWaits;
  }

  public void setMaxNumWaits(int maxNumWaits)
  {
    this.maxNumWaits = maxNumWaits;
  }
}
