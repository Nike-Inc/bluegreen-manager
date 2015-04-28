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

  /**
   * Sleep longer than this threshold is "long" from the perspective of someone watching the logs live, and
   * merits an extra line of debug logging.
   */
  private static final long LONG_SLEEP_THRESHOLD = 30000L; //30sec

  private WaiterParameters waiterParameters;
  private ThreadSleeper threadSleeper;
  private ProgressChecker<T> progressChecker;

  public Waiter(WaiterParameters waiterParameters,
                ThreadSleeper threadSleeper,
                ProgressChecker<T> progressChecker)
  {
    this.waiterParameters = waiterParameters;
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
    while (waitNum < waiterParameters.getMaxNumWaits() + 1) //Not counting "waitNum#0" since first one doesn't call sleep()
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
        LOGGER.info("Done: " + progressChecker.getDescription() + " ... time elapsed: " + stopWatch.toString());
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
    if (0 < waitNum && waitNum < waiterParameters.getMaxNumWaits() + 1)
    {
      if (waitNum % waiterParameters.getWaitReportInterval() == 0)
      {
        LOGGER.info("Wait #" + waitNum + " (max " + waiterParameters.getMaxNumWaits() + ") for "
            + progressChecker.getDescription() + " ... time elapsed: " + stopWatch.toString());
      }
      final long delay = waitNum == 1 ? waiterParameters.getInitialWaitDelayMilliseconds()
          : waiterParameters.getFollowupWaitDelayMilliseconds();
      if (delay >= LONG_SLEEP_THRESHOLD)
      {
        LOGGER.debug("Going to sleep for " + delay + " milliseconds");
      }
      try
      {
        threadSleeper.sleep(delay);
      }
      catch (InterruptedException e) //NOSONAR
      {
        LOGGER.warn("Sleep was interrupted");
      }
    }
  }

}
