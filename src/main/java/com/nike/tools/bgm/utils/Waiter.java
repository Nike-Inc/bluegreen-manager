package com.nike.tools.bgm.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A waiter comes in after you've done some initial command, and then waits iteratively until the initial command
 * done.  During each iteration the waiter executes a progress-checking command and displays the progress.
 * <p/>
 * When the ongoing external operation has reached its natural conclusion, we expect a result object of type T.
 */
@Scope("prototype")
@Component
public class Waiter<T>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(Waiter.class);

  private int maxNumWaits;
  private int waitReportInterval; //Set to 1 to report time elapsed on every iteration, or 10 for every 10th iteration.
  private long waitDelayMilliseconds;
  private ThreadSleeper threadSleeper;
  private ProgressChecker<T> progressChecker;

  public Waiter(int maxNumWaits,
                int waitReportInterval,
                long waitDelayMilliseconds,
                ThreadSleeper threadSleeper, ProgressChecker<T> progressChecker)
  {
    this.maxNumWaits = maxNumWaits;
    this.waitReportInterval = waitReportInterval;
    this.waitDelayMilliseconds = waitDelayMilliseconds;
    this.threadSleeper = threadSleeper;
    this.progressChecker = progressChecker;
  }

  /**
   * Waits (blocking) til the progressChecker says we are done, or until an uncaught error occurs, or until timeout.
   *
   * @return The result object when the progressChecker determines that we have reached a conclusion.
   */
  public T waitTilDone()
  {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    int waitNum = 0;
    while (waitNum < maxNumWaits + 1) //Not counting "waitNum#0" since first one doesn't call sleep()
    {
      if (waitNum == 0)
      {
        progressChecker.initialCheck();
      }
      else
      {
        progressChecker.followupCheck(waitNum);
      }
      if (progressChecker.isDone())
      {
        return progressChecker.getResult();
      }
      ++waitNum;
      sleep(waitNum, stopWatch);
    }
    return progressChecker.timeout();
  }

  /**
   * Sleeps for the wait delay, and catches interrupt exceptions.
   */
  private void sleep(int waitNum, StopWatch stopWatch)
  {
    if (0 < waitNum && waitNum < maxNumWaits + 1)
    {
      if (waitNum % waitReportInterval == 0)
      {
        LOGGER.info("Wait #" + waitNum + " (max " + maxNumWaits + ") for " + progressChecker.getDescription()
            + " ... time elapsed: " + stopWatch.toString());
      }
      try
      {
        threadSleeper.sleep(waitDelayMilliseconds);
      }
      catch (InterruptedException e) //NOSONAR
      {
        LOGGER.warn("Sleep was interrupted");
      }
    }
  }

}
