package com.nike.tools.bgm.tasks;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeProgress;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;

/**
 * Freezes the apps in the requested environment.
 */
@Lazy
@Component
public class FreezeTask extends TaskImpl
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FreezeTask.class);
  private static final long WAIT_DELAY_MILLISECONDS = 3000L;

  private static int maxNumWaits = 120;
  private static int waitReportInterval = 10;

  @Autowired
  private ApplicationClient applicationClient;

  @Autowired
  private ThreadSleeper threadSleeper;

  @Autowired
  private EnvironmentTx environmentTx;

  private Environment environment;
  private ApplicationVm applicationVm;
  private Application application;

  /**
   * Looks up the environment entity by name.
   * Currently requires that the env has exactly one applicationVm and one application.
   *
   * @return Self, so job can construct, init, and add to task list in one line.
   */
  public FreezeTask init(int position, String envName)
  {
    super.init(position);
    this.environment = environmentTx.activeLoadEnvironmentAndApplications(envName);

    getApplicationVmFromEnvironment();
    getApplicationFromVm();
    return this;
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   */
  String context()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[environment '" + environment.getEnvName() + "'");
    if (applicationVm != null)
    {
      sb.append(", ");
      if (application == null)
      {
        sb.append(applicationVm.getHostname());
      }
      else
      {
        sb.append(application.makeHostnameUri());
      }
    }
    sb.append("]: ");
    return sb.toString();
  }

  /**
   * Returns a tiny string pointing out if we're in noop.
   */
  private String noopRemark(boolean noop)
  {
    return noop ? " (noop)" : "";
  }

  /**
   * Gets the env's persisted application vm record.  (Currently support only 1.)
   */
  private void getApplicationVmFromEnvironment()
  {
    List<ApplicationVm> applicationVms = environment.getApplicationVms();
    if (CollectionUtils.isEmpty(applicationVms))
    {
      throw new IllegalStateException(context() + "No application vms, cannot freeze");
    }
    else if (applicationVms.size() > 1)
    {
      throw new UnsupportedOperationException(context() + "Currently only support case of 1 appvm, but found "
          + applicationVms.size());
    }
    this.applicationVm = applicationVms.get(0);
  }

  /**
   * Gets the env's persisted application record.  (Currently support only 1.)
   */
  private void getApplicationFromVm()
  {
    List<Application> applications = applicationVm.getApplications();
    if (CollectionUtils.isEmpty(applications))
    {
      throw new IllegalStateException(context() + "No applications, cannot freeze");
    }
    else if (applications.size() > 1)
    {
      throw new UnsupportedOperationException(context() + "Currently only support case of 1 application, but found "
          + applications.size());
    }
    this.application = applications.get(0);
  }

  /**
   * Attempts to freeze the target application, waits for freeze to be done.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    TaskStatus taskStatus = TaskStatus.ERROR;
    if (appIsReadyToFreeze())
    {
      DbFreezeProgress initialProgress = requestFreeze(noop);
      if (noop || initialProgress != null)
      {
        if (waitForFreeze(noop, initialProgress))
        {
          taskStatus = noop ? TaskStatus.NOOP : TaskStatus.DONE;
        }
      }
    }
    return taskStatus;
  }

  /**
   * Returns true if the application is ready to freeze.  (Normal and flush-error modes are accepted.)
   * <p/>
   * Read-only so runs even if noop.
   */
  boolean appIsReadyToFreeze()
  {
    LOGGER.info(context() + "Checking if application is ready to freeze");
    DbFreezeProgress dbFreezeProgress = applicationClient.getDbFreezeProgress(application);
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
      if (mode != DbFreezeMode.NORMAL && mode != DbFreezeMode.FLUSH_ERROR)
      {
        LOGGER.error(context() + "Mode '" + mode + "' indicates application is not ready to freeze.  Progress: "
            + dbFreezeProgress);
      }
      else
      {
        isReady = true;
      }
    }
    return isReady;
  }

  /**
   * Requests that the application enter freeze mode.
   *
   * @param noop If true, don't contact the application.
   * @return Initial progress if we got a successful response from the application, or null if noop.
   */
  DbFreezeProgress requestFreeze(boolean noop)
  {
    LOGGER.info(context() + "Requesting a freeze" + noopRemark(noop));
    DbFreezeProgress dbFreezeProgress = null;
    if (!noop)
    {
      dbFreezeProgress = applicationClient.putEnterDbFreeze(application);
      dbFreezeProgress = nullIfErrorProgress(dbFreezeProgress, 0);
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
   * Requests that the application enter freeze mode.
   *
   * @param noop If true, don't contact the application.
   * @return True if the application has been frozen prior to timeout, or if noop.  False if error or other failure to freeze.
   */
  boolean waitForFreeze(boolean noop, DbFreezeProgress initialProgress)
  {
    LOGGER.info(context() + "Waiting for freeze to take effect" + noopRemark(noop));
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
        switch (mode)
        {
          case FROZEN:
            LOGGER.info("Application is successfully frozen");
            return true;
          case FLUSH_ERROR:
            // Probably will never get here, nullIfErrorProgress will warn and set mode==null
            LOGGER.error("Application responded with flush error");
            return false;
          case FLUSHING:
            LOGGER.info("Application is flushing");
            break; //Expected response, keep trying.
          default:
            LOGGER.error("Application has reached unexpected mode '" + mode + "'");
            return false;
        }
        ++waitNum;
      }
      LOGGER.error("Application failed to respond with frozen mode prior to timeout");
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
        LOGGER.info("Wait #" + waitNum + " (max " + maxNumWaits + ") for application freeze ... "
            + ((waitNum * WAIT_DELAY_MILLISECONDS) / 1000L) + " seconds elapsed");
      }
      sleep();
      DbFreezeProgress dbFreezeProgress = applicationClient.getDbFreezeProgress(application);
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
    catch (InterruptedException e)
    {
      LOGGER.warn("Sleep was interrupted");
    }
  }

  // Test purposes only
  static void setWaitReportInterval(int waitReportInterval)
  {
    FreezeTask.waitReportInterval = waitReportInterval;
  }

  // Test purposes only
  static void setMaxNumWaits(int maxNumWaits)
  {
    FreezeTask.maxNumWaits = maxNumWaits;
  }
}
