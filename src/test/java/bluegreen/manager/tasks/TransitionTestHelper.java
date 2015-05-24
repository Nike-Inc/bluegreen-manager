package bluegreen.manager.tasks;

import bluegreen.manager.client.app.DbFreezeMode;
import bluegreen.manager.client.app.DbFreezeProgress;
import bluegreen.manager.client.app.DbFreezeRest;
import static bluegreen.manager.utils.TimeFakery.START_TIME_STRING;

public class TransitionTestHelper
{
  // Same values as used by ThawTaskTest

  public static final String VERB = "doSomeTransition";
  public static final DbFreezeMode[] ALLOWED_START_MODES = new DbFreezeMode[] { DbFreezeMode.FROZEN, DbFreezeMode.THAW_ERROR };
  public static final DbFreezeMode TRANSITIONAL_MODE = DbFreezeMode.THAW;
  public static final DbFreezeMode DESTINATION_MODE = DbFreezeMode.NORMAL;
  public static final DbFreezeMode TRANSITION_ERROR_MODE = DbFreezeMode.THAW_ERROR;
  public static final String TRANSITION_METHOD_PATH = DbFreezeRest.PUT_EXIT_DB_FREEZE;

  public static final TransitionParameters TRANSITION_PARAMETERS = new TransitionParameters(
      VERB, ALLOWED_START_MODES, TRANSITIONAL_MODE, DESTINATION_MODE, TRANSITION_ERROR_MODE, TRANSITION_METHOD_PATH
  );

  private static final String PROGRESS_USER = "someUser";

  /**
   * Makes a fake progress object showing a lock error.
   */
  public DbFreezeProgress fakeLockErrorProgress()
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setLockError(true);
    return progress;
  }

  /**
   * Makes a fake progress object showing a transition error.
   */
  public DbFreezeProgress fakeTransitionErrorProgress(DbFreezeMode transitionErrorMode)
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setMode(transitionErrorMode);
    progress.setUsername(PROGRESS_USER);
    progress.setStartTime(START_TIME_STRING);
    progress.setEndTime(START_TIME_STRING);
    progress.setTransitionError("There was an error!");
    return progress;
  }

  /**
   * Makes a fake progress object at a given mode (no errors).
   */
  public DbFreezeProgress fakeProgress(DbFreezeMode mode)
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setMode(mode);
    progress.setUsername(PROGRESS_USER);
    progress.setStartTime(START_TIME_STRING);
    progress.setEndTime(START_TIME_STRING);
    return progress;
  }

}
