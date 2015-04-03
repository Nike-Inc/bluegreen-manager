package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeProgress;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;

/**
 * Transitions the apps in the requested environment to the next dbfreeze-related steady state.
 */
public abstract class TransitionTask extends ApplicationTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(TransitionTask.class);
  private static final long WAIT_DELAY_MILLISECONDS = 3000L;

  private static int maxNumWaits = 120;
  private static int waitReportInterval = 10;

  @Autowired
  private ThreadSleeper threadSleeper;

  private ApplicationSession applicationSession;

  private TransitionParameters transitionParameters;

  /**
   * Finishes initializing the task.  (Mockito prevents it from being all done in constructor.)
   *
   * @return Self, so job can construct, init, and add to task list in one line.
   */
  public abstract TransitionTask initTransition(int position, String envName);

  /**
   * Looks up the environment entity by name.
   * Currently requires that the env has exactly one applicationVm and one application.
   */
  protected void init(int position, String envName, TransitionParameters transitionParameters)
  {
    super.init(position, envName);
    this.transitionParameters = transitionParameters;
  }

  /**
   * Returns a tiny string pointing out if we're in noop.
   */
  private String noopRemark(boolean noop)
  {
    return noop ? " (noop)" : "";
  }

  /**
   * Initializes an authenticated session with the application.
   */
  void initApplicationSession()
  {
    applicationSession = applicationClient.authenticate(application);
  }

  /**
   * Attempts to transition the target application, waits for it to finish.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    TaskStatus taskStatus = TaskStatus.ERROR;
    initApplicationSession();
    if (appIsReadyToTransition())
    {
      DbFreezeProgress initialProgress = requestTransition(noop);
      if (noop || initialProgress != null)
      {
        if (waitForTransition(noop, initialProgress))
        {
          taskStatus = noop ? TaskStatus.NOOP : TaskStatus.DONE;
        }
      }
    }
    return taskStatus;
  }

  /**
   * Returns true if the application is ready to make the transition.
   * <p/>
   * Read-only so runs even if noop.
   */
  boolean appIsReadyToTransition()
  {
    LOGGER.info(context() + "Checking if application is ready to " + transitionParameters.getVerb());
    DbFreezeProgress dbFreezeProgress = applicationClient.getDbFreezeProgress(application, applicationSession, null);
    LOGGER.debug(context() + "Application response: " + dbFreezeProgress);
    boolean isReady = false;
    if (dbFreezeProgress == null)
    {
      LOGGER.error(context() + "Null application response");
    }
    else if (dbFreezeProgress.isLockError())
    {
      LOGGER.error(context() + "Application responded with a lock error: " + dbFreezeProgress);
    }
    else
    {
      DbFreezeMode mode = dbFreezeProgress.getMode();
      if (!isAllowedStartMode(mode))
      {
        LOGGER.error(context() + "Mode '" + mode + "' indicates application is not ready to "
            + transitionParameters.getVerb() + ".  Progress: " + dbFreezeProgress);
      }
      else
      {
        isReady = true;
      }
    }
    return isReady;
  }

  /**
   * True if the transition parameters allow the transition to start from the specified mode.
   */
  boolean isAllowedStartMode(DbFreezeMode mode)
  {
    return ArrayUtils.contains(transitionParameters.getAllowedStartModes(), mode);
  }

  /**
   * Requests that the application do the transition.
   *
   * @param noop If true, don't contact the application.
   * @return Initial progress if we got a successful response from the application, or null if noop.
   */
  DbFreezeProgress requestTransition(boolean noop)
  {
    LOGGER.info(context() + "Requesting a " + transitionParameters.getVerb() + noopRemark(noop));
    DbFreezeProgress dbFreezeProgress = null;
    if (!noop)
    {
      final int waitNum = 0;
      dbFreezeProgress = applicationClient.putRequestTransition(application, applicationSession,
          transitionParameters.getTransitionMethodPath(), waitNum);
      dbFreezeProgress = nullIfErrorProgress(dbFreezeProgress, waitNum);
    }
    return dbFreezeProgress;
  }

  /**
   * Returns the input progress object, or null if there is any kind of error.  If error then log about it too.
   */
  private DbFreezeProgress nullIfErrorProgress(DbFreezeProgress dbFreezeProgress, int waitNum)
  {
    LOGGER.debug(context() + "Application response after wait#" + waitNum + ": " + dbFreezeProgress);
    if (dbFreezeProgress == null)
    {
      LOGGER.error(context() + "Null application response");
    }
    else if (dbFreezeProgress.isLockError())
    {
      LOGGER.error(context() + "Application responded with a lock error: " + dbFreezeProgress);
    }
    else if (StringUtils.isNotBlank(dbFreezeProgress.getTransitionError()))
    {
      LOGGER.error(context() + "Application responded with a transition error: " + dbFreezeProgress);
    }
    else
    {
      return dbFreezeProgress;
    }
    return null;
  }

  /**
   * Waits for the application to finish the requested transition.
   *
   * @param noop If true, don't contact the application.
   * @return True if the application has reached destination mode prior to timeout, or if noop.
   * False if error or other failure to make transition.
   */
  boolean waitForTransition(boolean noop, DbFreezeProgress initialProgress)
  {
    LOGGER.info(context() + "Waiting for " + transitionParameters.getVerb() + " to take effect" + noopRemark(noop));
    if (!noop)
    {
      int waitNum = 0;
      while (waitNum < maxNumWaits + 1) //Not counting "waitNum#0" since first one doesn't call sleep()
      {
        DbFreezeMode mode = waitNum == 0 ? initialProgress.getMode() : waitAndGetMode(waitNum);
        if (mode == null)
        {
          // Application error, already logged.
          // ApplicationClient already made MAX_NUM_TRIES, so mode==null means don't try again, client has a big problem.
          return false;
        }
        else if (mode == transitionParameters.getDestinationMode())
        {
          LOGGER.info("Application successfully reached destination mode '" + transitionParameters.getDestinationMode() + "'");
          return true;
        }
        else if (mode == transitionParameters.getTransitionErrorMode())
        {
          // Probably will never get here, nullIfErrorProgress will warn and set mode==null
          LOGGER.error("Application responded with transition error '" + transitionParameters.getTransitionErrorMode() + "'");
          return false;
        }
        else if (mode == transitionParameters.getTransitionalMode())
        {
          LOGGER.info("Application is in transitional mode '" + transitionParameters.getTransitionalMode() + "'");
          //Expected response, keep trying.
        }
        else
        {
          LOGGER.error("Application has reached unexpected mode '" + mode + "'");
          return false;
        }
        ++waitNum;
      }
      LOGGER.error("Application failed to reach destination mode '" + transitionParameters.getDestinationMode()
          + "' prior to timeout");
      return false;
    }
    return true;
  }

  /**
   * Waits and then gets the application's current progress mode.
   */
  private DbFreezeMode waitAndGetMode(int waitNum)
  {
    if (waitNum > 0)
    {
      if (waitNum % waitReportInterval == 0)
      {
        // "Seconds elapsed" is approximate, since ApplicationClient can have its own sleep delay for each "wait" here.
        LOGGER.info("Wait #" + waitNum + " (max " + maxNumWaits + ") for application " + transitionParameters.getVerb()
            + " ... " + ((waitNum * WAIT_DELAY_MILLISECONDS) / 1000L) + " seconds elapsed");
      }
      sleep();
      DbFreezeProgress dbFreezeProgress = applicationClient.getDbFreezeProgress(application, applicationSession, waitNum);
      dbFreezeProgress = nullIfErrorProgress(dbFreezeProgress, waitNum);
      if (dbFreezeProgress != null)
      {
        return dbFreezeProgress.getMode();
      }
    }
    return null;
  }

  /**
   * Sleeps for the wait delay, and catches interrupt exceptions.
   */
  private void sleep()
  {
    LOGGER.debug("Going to sleep, will check again");
    try
    {
      threadSleeper.sleep(WAIT_DELAY_MILLISECONDS);
    }
    catch (InterruptedException e) //NOSONAR
    {
      LOGGER.warn("Sleep was interrupted");
    }
  }

  // Test purposes only
  static void setWaitReportInterval(int waitReportInterval)
  {
    TransitionTask.waitReportInterval = waitReportInterval;
  }

  // Test purposes only
  static void setMaxNumWaits(int maxNumWaits)
  {
    TransitionTask.maxNumWaits = maxNumWaits;
  }
}
