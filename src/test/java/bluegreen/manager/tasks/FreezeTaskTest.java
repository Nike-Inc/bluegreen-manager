package bluegreen.manager.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.client.app.DbFreezeMode;
import bluegreen.manager.client.app.DbFreezeProgress;
import bluegreen.manager.client.app.DbFreezeRest;
import bluegreen.manager.model.domain.TaskStatus;

@RunWith(MockitoJUnitRunner.class)
public class FreezeTaskTest extends TransitionTaskBaseTest
{
  @InjectMocks
  private FreezeTask freezeTask;

  @Before
  public void setUp()
  {
    setUp(freezeTask);
  }

  /**
   * Tests that a null client response means the app is not ready to freeze.
   */
  @Test
  public void testAppIsReadyToTransition_NullProgress()
  {
    testAppIsReadyToTransition_NullProgress(freezeTask);
  }

  /**
   * Tests that a lock error response means the app is not ready to freeze.
   */
  @Test
  public void testAppIsReadyToTransition_LockError()
  {
    testAppIsReadyToTransition_LockError(freezeTask);
  }

  /**
   * Tests that a response in the "wrong mode" means the app is not ready to freeze.
   */
  @Test
  public void testAppIsReadyToTransition_WrongMode()
  {
    testAppIsReadyToTransition_WrongMode(freezeTask, DbFreezeMode.FROZEN);
  }

  /**
   * Tests that a response in the normal mode means the app is ready to freeze.
   */
  @Test
  public void testAppIsReadyToTransition_AllowedStartMode()
  {
    testAppIsReadyToTransition_AllowedStartMode(freezeTask, DbFreezeMode.NORMAL);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  @Test
  public void testRequestTransition_Noop()
  {
    testRequestTransition_Noop(freezeTask, DbFreezeRest.PUT_ENTER_DB_FREEZE);
  }

  /**
   * Tests a null client response.
   */
  @Test
  public void testRequestTransition_NullProgress()
  {
    testRequestTransition_NullProgress(freezeTask, DbFreezeRest.PUT_ENTER_DB_FREEZE);
  }

  /**
   * Tests a lock error response.
   */
  @Test
  public void testRequestTransition_LockError()
  {
    testRequestTransition_LockError(freezeTask, DbFreezeRest.PUT_ENTER_DB_FREEZE);
  }

  /**
   * Tests a transition error response.
   */
  @Test
  public void testRequestTransition_TransitionError()
  {
    testRequestTransition_TransitionError(freezeTask, DbFreezeRest.PUT_ENTER_DB_FREEZE);
  }

  /**
   * Tests that a non-error response from the client gets passed as the service return value.
   */
  @Test
  public void testRequestTransition_Normal()
  {
    testRequestTransition_Normal(freezeTask, DbFreezeRest.PUT_ENTER_DB_FREEZE);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  @Test
  public void testWaitForTransition_Noop()
  {
    testWaitForTransition_Noop(freezeTask, DbFreezeRest.PUT_ENTER_DB_FREEZE);
  }

  /**
   * Tests waitForTransition with freeze-specific args.
   */
  private void testWaitForTransition_ThreeFlushingThenEnd(DbFreezeProgress fourthProgress,
                                                          boolean expectSuccess) throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(freezeTask, DbFreezeMode.FLUSHING, fourthProgress, expectSuccess);
  }

  /**
   * Tests the success case where we get freeze confirmation after the third wait (4th progress object).
   */
  @Test
  public void testWaitForTransition_SuccessOnThirdWait() throws InterruptedException
  {
    testWaitForTransition_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FROZEN), true);
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
    testWaitForTransition_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.THAW), false);
  }

  /**
   * Tests the case where the application is still in transitional mode when the timeout has elapsed.
   */
  @Test
  public void testWaitForTransition_TimeoutOnThirdWait() throws InterruptedException
  {
    fakeWaiterParameters.setMaxNumWaits(3);
    testWaitForTransition_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FLUSHING), false);
  }

  /**
   * Tests the case where noop returns NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    testProcess_Noop(freezeTask, DbFreezeMode.NORMAL);
  }

  /**
   * Tests case where the read-only actions of a noop cause an error.
   */
  @Test
  public void testProcess_NoopError()
  {
    testProcess_NoopError(freezeTask);
  }

  /**
   * Tests transition process() with freeze-specific args.
   */
  private void testProcess_ThreeFlushingThenEnd(DbFreezeProgress fourthProgress,
                                                TaskStatus expectedStatus) throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(freezeTask, DbFreezeMode.NORMAL, DbFreezeMode.FLUSHING,
        DbFreezeRest.PUT_ENTER_DB_FREEZE, fourthProgress, expectedStatus);
  }

  /**
   * Tests the success case where we get freeze confirmation after the third wait (4th progress object).
   */
  @Test
  public void testProcess_FrozenOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FROZEN), TaskStatus.DONE);
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
    testProcess_ThreeFlushingThenEnd(fakeTransitionErrorProgress(DbFreezeMode.FLUSH_ERROR), TaskStatus.ERROR);
  }

  /**
   * Tests the case where application enters an unexpected mode after the third wait (4th progress object).
   */
  @Test
  public void testProcess_WrongModeOnThirdWait() throws InterruptedException
  {
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.THAW), TaskStatus.ERROR);
  }

  /**
   * Tests the case where the application is still in transitional mode when the timeout has elapsed.
   */
  @Test
  public void testProcess_TimeoutOnThirdWait() throws InterruptedException
  {
    fakeWaiterParameters.setMaxNumWaits(3);
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FLUSHING), TaskStatus.ERROR);
  }

}