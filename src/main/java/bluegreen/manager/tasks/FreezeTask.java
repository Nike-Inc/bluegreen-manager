package bluegreen.manager.tasks;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bluegreen.manager.client.app.DbFreezeMode;
import bluegreen.manager.client.app.DbFreezeRest;

/**
 * Freezes the apps in the requested environment.
 */
@Lazy
@Component
@Scope("prototype")
public class FreezeTask extends TransitionTask
{
  private static final String VERB = "freeze";
  private static final DbFreezeMode[] ALLOWED_START_MODES = new DbFreezeMode[] { DbFreezeMode.NORMAL, DbFreezeMode.FLUSH_ERROR };
  private static final DbFreezeMode TRANSITIONAL_MODE = DbFreezeMode.FLUSHING;
  private static final DbFreezeMode DESTINATION_MODE = DbFreezeMode.FROZEN;
  private static final DbFreezeMode TRANSITION_ERROR_MODE = DbFreezeMode.FLUSH_ERROR;
  private static final String TRANSITION_METHOD_PATH = DbFreezeRest.PUT_ENTER_DB_FREEZE;

  private static final TransitionParameters TRANSITION_PARAMETERS = new TransitionParameters(
      VERB, ALLOWED_START_MODES, TRANSITIONAL_MODE, DESTINATION_MODE, TRANSITION_ERROR_MODE, TRANSITION_METHOD_PATH
  );

  @Override
  public TransitionTask assignTransition(int position, String envName)
  {
    assign(position, envName, TRANSITION_PARAMETERS);
    return this;
  }
}
