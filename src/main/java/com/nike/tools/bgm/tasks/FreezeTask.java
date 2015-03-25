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
import com.nike.tools.bgm.model.dao.EnvironmentDAO;
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
  private static final int MAX_NUM_WAITS = 120;
  private static final long WAIT_DELAY_MILLISECONDS = 3000L;
  private static final int WAIT_REPORT_INTERVAL = 10;

  @Autowired
  private ApplicationClient applicationClient;

  @Autowired
  private ThreadSleeper threadSleeper;

  @Autowired
  private EnvironmentDAO environmentDAO;

  private Environment environment;
  private ApplicationVm applicationVm;
  private Application application;

  /**
   * Constructor: looks up the environment entity by name.
   * Currently requires that the env has exactly one appvm and one app.
   */
  public FreezeTask(int position, String envName)
  {
    super(position);
    this.environment = environmentDAO.findNamedEnv(envName);

    getApplicationVmFromEnvironment();
    getApplicationFromVm();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   */
  private String context()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("environment '" + environment.getEnvName() + "': ");
    if (applicationVm != null)
    {
      sb.append(applicationVm + ": ");
      if (application != null)
      {
        sb.append(application + ": ");
      }
    }
    return sb.toString();
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
  private boolean appIsReadyToFreeze()
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
  private DbFreezeProgress requestFreeze(boolean noop)
  {
    LOGGER.info(context() + "Requesting a freeze");
    if (!noop)
    {
      DbFreezeProgress dbFreezeProgress = applicationClient.putEnterDbFreeze(application);
      LOGGER.debug(context() + "Application response: " + dbFreezeProgress);
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
    }
    return null;
  }

  /**
   * Requests that the application enter freeze mode.
   *
   * @param noop If true, don't contact the application.
   * @return True if the application has been frozen prior to timeout, or if noop.  False if error or other failure to freeze.
   */
  private boolean waitForFreeze(boolean noop, DbFreezeProgress initialProgress)
  {
    LOGGER.info(context() + "Waiting for freeze to take effect");
    if (!noop)
    {
      int waitNum = 0;
      while (waitNum < MAX_NUM_WAITS)
      {
        DbFreezeMode mode = waitNum == 0 ? initialProgress.getMode() : waitAndGetMode(waitNum);
        switch (mode)
        {
          case FROZEN:
            LOGGER.info("Application is successfully frozen");
            return true;
          case FLUSH_ERROR:
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
      if (waitNum % WAIT_REPORT_INTERVAL == 0)
      {
        LOGGER.info("Wait #" + waitNum + " (max " + MAX_NUM_WAITS + ") for application freeze");
      }
      sleep();
      DbFreezeProgress dbFreezeProgress = applicationClient.getDbFreezeProgress(application);
      LOGGER.debug(context() + "Application response #" + waitNum + ": " + dbFreezeProgress);
      return dbFreezeProgress.getMode();
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
}
