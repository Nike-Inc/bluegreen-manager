package bluegreen.manager.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bluegreen.manager.client.app.ApplicationClient;
import bluegreen.manager.client.app.ApplicationSession;
import bluegreen.manager.client.app.DbFreezeMode;
import bluegreen.manager.client.app.DbFreezeProgress;
import bluegreen.manager.model.domain.Application;
import bluegreen.manager.utils.ProgressChecker;

/**
 * Knows how to check progress of an Application undergoing dbfreeze transition.
 * <p/>
 * "Result": True if the application has reached destination mode prior to timeout.
 */
public class TransitionProgressChecker implements ProgressChecker<Boolean>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(TransitionProgressChecker.class);

  private TransitionParameters transitionParameters;
  private String logContext;
  private DbFreezeProgress initialProgress;
  protected ApplicationClient applicationClient;
  protected ApplicationSession applicationSession;
  protected Application application;
  private boolean done;
  private Boolean result;

  public TransitionProgressChecker(TransitionParameters transitionParameters,
                                   String logContext,
                                   DbFreezeProgress initialProgress,
                                   ApplicationClient applicationClient,
                                   ApplicationSession applicationSession,
                                   Application application)
  {
    this.transitionParameters = transitionParameters;
    this.logContext = logContext;
    this.initialProgress = initialProgress;
    this.applicationClient = applicationClient;
    this.applicationSession = applicationSession;
    this.application = application;
  }

  @Override
  public String getDescription()
  {
    return "application " + transitionParameters.getVerb();
  }

  /**
   * Checks initial mode, or concludes immediately if error.
   */
  @Override
  public void initialCheck()
  {
    LOGGER.debug(logContext + "Initial application response: " + initialProgress);
    DbFreezeProgress scrubbedProgress = nullIfErrorProgress(initialProgress, 0/*waitNum*/);
    if (scrubbedProgress != null)
    {
      checkMode(scrubbedProgress.getMode());
    }
    else
    {
      done = true;
      result = false;
    }
  }

  /**
   * Communicates with the application for updated progress and checks the mode.
   * Concludes if error or if naturally done.
   */
  @Override
  public void followupCheck(int waitNum)
  {
    DbFreezeProgress dbFreezeProgress = applicationClient.getDbFreezeProgress(application, applicationSession, waitNum);
    LOGGER.debug(logContext + "Application response after wait#" + waitNum + ": " + dbFreezeProgress);
    dbFreezeProgress = nullIfErrorProgress(dbFreezeProgress, waitNum);
    DbFreezeMode mode = null;
    if (dbFreezeProgress != null)
    {
      mode = dbFreezeProgress.getMode();
    }
    checkMode(mode);
  }

  /**
   * Performs the check, based on the application's dbfreeze mode.
   */
  private void checkMode(DbFreezeMode mode)
  {
    if (mode == null)
    {
      // Application error, already logged.
      // ApplicationClient already made MAX_NUM_TRIES, so mode==null means don't try again, client has a big problem.
      done = true;
      result = false;
    }
    else if (mode == transitionParameters.getDestinationMode())
    {
      LOGGER.info("Application successfully reached destination mode '" + transitionParameters.getDestinationMode() + "'");
      done = true;
      result = true;
    }
    else if (mode == transitionParameters.getTransitionErrorMode())
    {
      // Probably will never get here, nullIfErrorProgress will warn and set mode==null
      LOGGER.error("Application responded with transition error '" + transitionParameters.getTransitionErrorMode() + "'");
      done = true;
      result = false;
    }
    else if (mode == transitionParameters.getTransitionalMode())
    {
      LOGGER.debug("Application is in transitional mode '" + transitionParameters.getTransitionalMode() + "'");
      //Expected response, keep trying.
    }
    else
    {
      LOGGER.error("Application has reached unexpected mode '" + mode + "'");
      done = true;
      result = false;
    }
  }

  /**
   * Returns the input progress object, or null if there is any kind of error.  If error then log about it too.
   */
  private DbFreezeProgress nullIfErrorProgress(DbFreezeProgress dbFreezeProgress, int waitNum)
  {
    if (dbFreezeProgress == null)
    {
      LOGGER.error(logContext + "Null application response");
    }
    else if (dbFreezeProgress.isLockError())
    {
      LOGGER.error(logContext + "Application responded with a lock error: " + dbFreezeProgress);
    }
    else if (StringUtils.isNotBlank(dbFreezeProgress.getTransitionError()))
    {
      LOGGER.error(logContext + "Application responded with a transition error: " + dbFreezeProgress);
    }
    else
    {
      return dbFreezeProgress;
    }
    return null;
  }

  @Override
  public boolean isDone()
  {
    return done;
  }

  /**
   * True if the application has reached destination mode prior to timeout.
   * False for an error.  Null if transitional or timeout.
   */
  @Override
  public Boolean getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns false (since dest mode not reached).
   */
  @Override
  public Boolean timeout()
  {
    LOGGER.error("Application failed to reach destination mode '" + transitionParameters.getDestinationMode()
        + "' prior to timeout");
    return false;
  }

  //Test purposes only.
  DbFreezeProgress getInitialProgress()
  {
    return initialProgress;
  }
}
