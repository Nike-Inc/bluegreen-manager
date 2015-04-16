package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeProgress;
import com.nike.tools.bgm.client.app.DbFreezeRest;
import com.nike.tools.bgm.model.domain.TaskStatus;

@RunWith(MockitoJUnitRunner.class)
public class ThawTaskTest extends TransitionTaskBaseTest
{
  @InjectMocks
  private ThawTask thawTask;

  @Before
  public void setUp()
  {
    setUp(thawTask);
  }

  /**
   * Tests that a null client response means the app is not ready to thaw.
   */
  @Test
  public void testAppIsReadyToTransition_NullProgress()
  {
    testAppIsReadyToTransition_NullProgress(thawTask);
  }

  /**
   * Tests that a lock error response means the app is not ready to thaw.
   */
  @Test
  public void testAppIsReadyToTransition_LockError()
  {
    testAppIsReadyToTransition_LockError(thawTask);
  }

  /**
   * Tests that a response in the "wrong mode" means the app is not ready to thaw.
   */
  @Test
  public void testAppIsReadyToTransition_WrongMode()
  {
    testAppIsReadyToTransition_WrongMode(thawTask, DbFreezeMode.NORMAL);
  }

  /**
   * Tests that a response in the frozen mode means the app is ready to thaw.
   */
  @Test
  public void testAppIsReadyToTransition_AllowedStartMode()
  {
    testAppIsReadyToTransition_AllowedStartMode(thawTask, DbFreezeMode.FROZEN);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  @Test
  public void testRequestTransition_Noop()
  {
    testRequestTransition_Noop(thawTask, DbFreezeRest.PUT_EXIT_DB_FREEZE);
  }

  /**
   * Tests a null client response.
   */
  @Test
  public void testRequestTransition_NullProgress()
  {
    testRequestTransition_NullProgress(thawTask, DbFreezeRest.PUT_EXIT_DB_FREEZE);
  }

  /**
   * Tests a lock error response.
   */
  @Test
  public void testRequestTransition_LockError()
  {
    testRequestTransition_LockError(thawTask, DbFreezeRest.PUT_EXIT_DB_FREEZE);
  }

  /**
   * Tests a transition error response.
   */
  @Test
  public void testRequestTransition_TransitionError()
  {
    testRequestTransition_TransitionError(thawTask, DbFreezeRest.PUT_EXIT_DB_FREEZE);
  }

  /**
   * Tests that a non-error response from the client gets passed as the service return value.
   */
  @Test
  public void testRequestTransition_Normal()
  {
    testRequestTransition_Normal(thawTask, DbFreezeRest.PUT_EXIT_DB_FREEZE);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  @Test
  public void testWaitForTransition_Noop()
  {
    testWaitForTransition_Noop(thawTask, DbFreezeRest.PUT_EXIT_DB_FREEZE);
  }

  /**
   * Tests waitForTransition with thaw-specific args.
   */
  private void testWaitForTransition_ThreeFlushingThenEnd(DbFreezeProgress fourthProgress,
                                                          boolean expectSuccess) throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(thawTask, DbFreezeMode.THAW, fourthProgress, expectSuccess);
  }

  /**
   * Tests the success case where we get freeze confirmation after the third wait (4th progress object).
   */
  @Test
  public void testWaitForTransition_SuccessOnThirdWait() throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.NORMAL), true);
  }

  /**
   * Tests the case where application has a serious error after the third wait (4th progress object).
   */
  @Test
  public void testWaitForTransition_NullOnThirdWait() throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(null, false);
  }

  /**
   * Tests the case where application has a serious error after the third wait (4th progress object).
   */
  @Test
  public void testWaitForTransition_LockErrorOnThirdWait() throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(fakeLockErrorProgress(), false);
  }

  /**
   * Tests the case where application has a "less serious" transition error after the third wait (4th progress object).
   */
  @Test
  public void testWaitForTransition_TransitionErrorOnThirdWait() throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(fakeTransitionErrorProgress(DbFreezeMode.FLUSH_ERROR), false);
  }

  /**
   * Tests the case where application enters an unexpected mode after the third wait (4th progress object).
   */
  @Test
  public void testWaitForTransition_WrongModeOnThirdWait() throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FROZEN), false);
  }

  /**
   * Tests the case where the application is still in transitional mode when the timeout has elapsed.
   */
  @Test
  public void testWaitForTransition_TimeoutOnThirdWait() throws InterruptedException
  {
    fakeWaiterParameters.setMaxNumWaits(3);
    testWaitForTransition_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.THAW), false);
  }

  /**
   * Tests the case where noop returns NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    testProcess_Noop(thawTask, DbFreezeMode.FROZEN);
  }

  /**
   * Tests case where the read-only actions of a noop cause an error.
   */
  @Test
  public void testProcess_NoopError()
  {
    testProcess_NoopError(thawTask);
  }

  /**
   * Tests transition process() with thaw-specific args.
   */
  private void testProcess_ThreeFlushingThenEnd(DbFreezeProgress fourthProgress,
                                                TaskStatus expectedStatus) throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(thawTask, DbFreezeMode.FROZEN, DbFreezeMode.THAW,
        DbFreezeRest.PUT_EXIT_DB_FREEZE, fourthProgress, expectedStatus);
  }

  /**
   * Tests the success case where we get thaw confirmation after the third wait (4th progress object).
   */
  @Test
  public void testProcess_FrozenOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.NORMAL), TaskStatus.DONE);
  }

  /**
   * Tests the case where application has a serious error after the third wait (4th progress object).
   */
  @Test
  public void testProcess_NullOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(null, TaskStatus.ERROR);
  }

  /**
   * Tests the case where application has a serious error after the third wait (4th progress object).
   */
  @Test
  public void testProcess_LockErrorOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(fakeLockErrorProgress(), TaskStatus.ERROR);
  }

  /**
   * Tests the case where application has a "less serious" transition error after the third wait (4th progress object).
   */
  @Test
  public void testProcess_TransitionErrorOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(fakeTransitionErrorProgress(DbFreezeMode.THAW_ERROR), TaskStatus.ERROR);
  }

  /**
   * Tests the case where application enters an unexpected mode after the third wait (4th progress object).
   */
  @Test
  public void testProcess_WrongModeOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FLUSHING), TaskStatus.ERROR);
  }

  /**
   * Tests the case where the application is still in transitional mode when the timeout has elapsed.
   */
  @Test
  public void testProcess_TimeoutOnThirdWait() throws InterruptedException
  {
    fakeWaiterParameters.setMaxNumWaits(3);
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.THAW), TaskStatus.ERROR);
  }

}
