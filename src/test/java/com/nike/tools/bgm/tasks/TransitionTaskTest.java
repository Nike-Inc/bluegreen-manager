package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.DbFreezeMode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the part of TransitionTask that doesn't need real freeze/thaw configuration.
 */
@RunWith(MockitoJUnitRunner.class)
public class TransitionTaskTest extends TransitionTaskBaseTest
{
  @InjectMocks
  private TransitionTask transitionTask = new TransitionTask()
  {
    public TransitionTask assignTransition(int position, String envName)
    {
      assign(position, envName, TransitionTestHelper.TRANSITION_PARAMETERS);
      return this;
    }
  };

  @Before
  public void setUp()
  {
    setUp(transitionTask);
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