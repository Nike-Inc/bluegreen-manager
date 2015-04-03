package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeRest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the part of TransitionTask that doesn't need real freeze/thaw configuration.
 */
@RunWith(MockitoJUnitRunner.class)
public class TransitionTaskTest extends TransitionTaskBaseTest
{
  private static final String VERB = "doSomeTransition";
  private static final DbFreezeMode[] ALLOWED_START_MODES = new DbFreezeMode[] { DbFreezeMode.FROZEN, DbFreezeMode.THAW_ERROR };
  private static final DbFreezeMode TRANSITIONAL_MODE = DbFreezeMode.THAW;
  private static final DbFreezeMode DESTINATION_MODE = DbFreezeMode.NORMAL;
  private static final DbFreezeMode TRANSITION_ERROR_MODE = DbFreezeMode.THAW_ERROR;
  private static final String TRANSITION_METHOD_PATH = DbFreezeRest.PUT_EXIT_DB_FREEZE;

  private static final TransitionParameters TRANSITION_PARAMETERS = new TransitionParameters(
      VERB, ALLOWED_START_MODES, TRANSITIONAL_MODE, DESTINATION_MODE, TRANSITION_ERROR_MODE, TRANSITION_METHOD_PATH
  );

  @InjectMocks
  private TransitionTask transitionTask = new TransitionTask()
  {
    public TransitionTask init(int position, String envName)
    {
      init(position, envName, TRANSITION_PARAMETERS);
      return this;
    }
  };

  @Before
  public void setUp()
  {
    setUp(transitionTask);
  }

  /**
   * Tests that we can make a nice env/vm/app context string for logging.
   */
  @Test
  public void testContext()
  {
    String str = transitionTask.context();
    assertTrue(str.contains("environment"));
    assertTrue(str.contains("http"));
  }

  /**
   * Tests that the task can be parameterized with a list of allowed start modes.
   */
  @Test
  public void testIsAllowedStartMode()
  {
    assertTrue(transitionTask.isAllowedStartMode(DbFreezeMode.FROZEN));
    assertTrue(transitionTask.isAllowedStartMode(DbFreezeMode.THAW_ERROR));
    assertFalse(transitionTask.isAllowedStartMode(DbFreezeMode.NORMAL));
  }
}