package bluegreen.manager.tasks;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.client.app.DbFreezeMode;
import bluegreen.manager.client.app.DbFreezeRest;

/**
 * Thaws the apps in the requested environment.
 */
@Lazy
@Component
public class ThawTask extends TransitionTask
{
  private static final String VERB = "thaw";
  private static final DbFreezeMode[] ALLOWED_START_MODES = new DbFreezeMode[] { DbFreezeMode.FROZEN, DbFreezeMode.THAW_ERROR };
  private static final DbFreezeMode TRANSITIONAL_MODE = DbFreezeMode.THAW;
  private static final DbFreezeMode DESTINATION_MODE = DbFreezeMode.NORMAL;
  private static final DbFreezeMode TRANSITION_ERROR_MODE = DbFreezeMode.THAW_ERROR;
  private static final String TRANSITION_METHOD_PATH = DbFreezeRest.PUT_EXIT_DB_FREEZE;

  private static final TransitionParameters TRANSITION_PARAMETERS = new TransitionParameters(
      VERB, ALLOWED_START_MODES, TRANSITIONAL_MODE, DESTINATION_MODE, TRANSITION_ERROR_MODE, TRANSITION_METHOD_PATH
  );

  /**
   * Finishes initializing the task.  (Mockito prevents it from being all done in constructor.)
   *
   * @return Self, so job can construct, assign, and add to task list in one line.
   */
  @Override
  public TransitionTask assignTransition(int position, String envName)
  {
    assign(position, envName, TRANSITION_PARAMETERS);
    return this;
  }
}
