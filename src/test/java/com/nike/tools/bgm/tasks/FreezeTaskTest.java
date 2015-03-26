package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeProgress;
import com.nike.tools.bgm.model.dao.EnvironmentDAO;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIME_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FreezeTaskTest
{
  private static final Application FAKE_APPLICATION = ApplicationTestHelper.makeFakeApplication();
  private static final String PROGRESS_USER = "someUser";

  @InjectMocks
  private FreezeTask freezeTask;

  @Mock
  private ApplicationClient mockApplicationClient;

  @Mock
  private ThreadSleeper mockThreadSleeper;

  @Mock
  private EnvironmentDAO mockEnvironmentDAO;

  /**
   * Tests init's ability to get env, applicationVm and application.
   */
  @Before
  public void setUp()
  {
    String envName = FAKE_APPLICATION.getApplicationVm().getEnvironment().getEnvName();
    when(mockEnvironmentDAO.findNamedEnv(envName)).thenReturn(FAKE_APPLICATION.getApplicationVm().getEnvironment());
    freezeTask.init(1, envName);
  }

  /**
   * Tests that we can make a nice env/vm/app context string for logging.
   */
  @Test
  public void testContext()
  {
    String str = freezeTask.context();
    assertTrue(str.contains("environment"));
    assertTrue(str.contains("http"));
  }

  /**
   * Makes a fake progress object showing a lock error.
   */
  private DbFreezeProgress fakeLockErrorProgress()
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setLockError(true);
    return progress;
  }

  /**
   * Makes a fake progress object showing a transition error.
   */
  private DbFreezeProgress fakeTransitionErrorProgress()
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setMode(DbFreezeMode.FLUSH_ERROR);
    progress.setUsername(PROGRESS_USER);
    progress.setStartTime(START_TIME_STRING);
    progress.setEndTime(START_TIME_STRING);
    progress.setTransitionError("There was an error!");
    return progress;
  }

  private DbFreezeProgress fakeProgress(DbFreezeMode mode)
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setMode(mode);
    progress.setUsername(PROGRESS_USER);
    progress.setStartTime(START_TIME_STRING);
    progress.setEndTime(START_TIME_STRING);
    return progress;
  }

  /**
   * Tests that a null client response means the app is not ready to freeze.
   */
  @Test
  public void testAppIsReadyToFreeze_NullProgress()
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION)).thenReturn(null);

    boolean isReady = freezeTask.appIsReadyToFreeze();

    assertFalse(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION);
  }

  /**
   * Tests that a lock error response means the app is not ready to freeze.
   */
  @Test
  public void testAppIsReadyToFreeze_LockError()
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION)).thenReturn(fakeLockErrorProgress());

    boolean isReady = freezeTask.appIsReadyToFreeze();

    assertFalse(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION);
  }

  /**
   * Tests that a response in the "wrong mode" means the app is not ready to freeze.
   */
  @Test
  public void testAppIsReadyToFreeze_WrongMode()
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION)).thenReturn(fakeProgress(DbFreezeMode.FROZEN));

    boolean isReady = freezeTask.appIsReadyToFreeze();

    assertFalse(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION);
  }

  /**
   * Tests that a response in the normal mode means the app is ready to freeze.
   */
  @Test
  public void testAppIsReadyToFreeze_Normal()
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION)).thenReturn(fakeProgress(DbFreezeMode.NORMAL));

    boolean isReady = freezeTask.appIsReadyToFreeze();

    assertTrue(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  @Test
  public void testRequestFreeze_Noop()
  {
    DbFreezeProgress progress = freezeTask.requestFreeze(true);

    assertNull(progress);
    verifyZeroInteractions(mockApplicationClient);
  }

  /**
   * Tests a null client response.
   */
  @Test
  public void testRequestFreeze_NullProgress()
  {
    when(mockApplicationClient.putEnterDbFreeze(FAKE_APPLICATION)).thenReturn(null);

    DbFreezeProgress progress = freezeTask.requestFreeze(false);

    assertNull(progress);
    verify(mockApplicationClient).putEnterDbFreeze(FAKE_APPLICATION);
  }

  /**
   * Tests a lock error response.
   */
  @Test
  public void testRequestFreeze_LockError()
  {
    when(mockApplicationClient.putEnterDbFreeze(FAKE_APPLICATION)).thenReturn(null);

    DbFreezeProgress progress = freezeTask.requestFreeze(false);

    assertNull(progress);
    verify(mockApplicationClient).putEnterDbFreeze(FAKE_APPLICATION);
  }

  /**
   * Tests a transition error response.
   */
  @Test
  public void testRequestFreeze_TransitionError()
  {
    when(mockApplicationClient.putEnterDbFreeze(FAKE_APPLICATION)).thenReturn(fakeTransitionErrorProgress());

    DbFreezeProgress progress = freezeTask.requestFreeze(false);

    assertNull(progress);
    verify(mockApplicationClient).putEnterDbFreeze(FAKE_APPLICATION);
  }

  /**
   * Tests that a non-error response from the client gets passed as the service return value.
   */
  @Test
  public void testRequestFreeze_Normal()
  {
    when(mockApplicationClient.putEnterDbFreeze(FAKE_APPLICATION)).thenReturn(fakeProgress(DbFreezeMode.FLUSHING));

    DbFreezeProgress progress = freezeTask.requestFreeze(false);

    assertNotNull(progress);
    verify(mockApplicationClient).putEnterDbFreeze(FAKE_APPLICATION);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  @Test
  public void testWaitForFreeze_Noop()
  {
    boolean ok = freezeTask.waitForFreeze(true, null);

    assertTrue(ok);
    verifyZeroInteractions(mockApplicationClient);
  }

  /**
   * Tests the case where we get a flushing response three times, followed by a fourth progress object that
   * ends the waiting.
   */
  private void testWaitForFreeze_ThreeFlushingThenEnd(DbFreezeProgress fourthProgress,
                                                      boolean expectFrozen) throws InterruptedException
  {
    freezeTask.setWaitReportInterval(2); //Just to see the extra logging on progress object #2, can't assert it though
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION))
        .thenReturn(fakeProgress(DbFreezeMode.FLUSHING)) //progress #1, after 1st wait
        .thenReturn(fakeProgress(DbFreezeMode.FLUSHING)) //progress #2, after 2nd wait
        .thenReturn(fourthProgress);                     //progress #3, after 3rd wait

    boolean isFrozen = freezeTask.waitForFreeze(false, fakeProgress(DbFreezeMode.FLUSHING)/*progress #0*/);

    assertEquals(expectFrozen, isFrozen);
    verify(mockApplicationClient, times(3)).getDbFreezeProgress(FAKE_APPLICATION);
    verify(mockThreadSleeper, times(3)).sleep(anyLong());
  }

  /**
   * Tests the success case where we get freeze confirmation after the third wait (4th progress object).
   */
  @Test
  public void testWaitForFreeze_FrozenOnThirdWait() throws InterruptedException
  {
    testWaitForFreeze_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FROZEN), true);
  }

  /**
   * Tests the case where application has a serious error after the third wait (4th progress object).
   */
  @Test
  public void testWaitForFreeze_NullOnThirdWait() throws InterruptedException
  {
    testWaitForFreeze_ThreeFlushingThenEnd(null, false);
  }

  /**
   * Tests the case where application has a serious error after the third wait (4th progress object).
   */
  @Test
  public void testWaitForFreeze_LockErrorOnThirdWait() throws InterruptedException
  {
    testWaitForFreeze_ThreeFlushingThenEnd(fakeLockErrorProgress(), false);
  }

  /**
   * Tests the case where application has a "less serious" transition error after the third wait (4th progress object).
   */
  @Test
  public void testWaitForFreeze_TransitionErrorOnThirdWait() throws InterruptedException
  {
    testWaitForFreeze_ThreeFlushingThenEnd(fakeTransitionErrorProgress(), false);
  }

  /**
   * Tests the case where application enters an unexpected mode after the third wait (4th progress object).
   */
  @Test
  public void testWaitForFreeze_WrongModeOnThirdWait() throws InterruptedException
  {
    testWaitForFreeze_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.THAW), false);
  }

  /**
   * Tests the case where the application does not freeze before the timeout has elapsed.
   */
  @Test
  public void testWaitForFreeze_TimeoutOnThirdWait() throws InterruptedException
  {
    freezeTask.setMaxNumWaits(3);
    testWaitForFreeze_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FLUSHING), false);
    //Normally it would continue waiting if application is still FLUSHING
  }

  /**
   * Tests the case where noop returns NOOP.
   */
  @Test
  public void testProcess_Noop()
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION)).thenReturn(fakeProgress(DbFreezeMode.NORMAL));

    TaskStatus taskStatus = freezeTask.process(true);

    assertEquals(TaskStatus.NOOP, taskStatus);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION);
  }

  /**
   * Tests case where the read-only actions of a noop cause an error.
   */
  @Test
  public void testProcess_NoopError()
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION)).thenReturn(null);

    TaskStatus taskStatus = freezeTask.process(true);

    assertEquals(TaskStatus.ERROR, taskStatus);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION);
  }

  /**
   * Tests the case where we get a flushing response three times, followed by a fourth progress object that
   * ends the waiting.  (Technically there are five total progress objects because initial state is also "progress.")
   */
  private void testProcess_ThreeFlushingThenEnd(DbFreezeProgress fourthProgress,
                                                TaskStatus expectedStatus) throws InterruptedException
  {
    freezeTask.setWaitReportInterval(2); //Just to see the extra logging on progress object #2, can't assert it though
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION))
        .thenReturn(fakeProgress(DbFreezeMode.NORMAL))   //initial state ...before enterFreeze
        .thenReturn(fakeProgress(DbFreezeMode.FLUSHING)) //progress #1, after 1st wait
        .thenReturn(fakeProgress(DbFreezeMode.FLUSHING)) //progress #2, after 2nd wait
        .thenReturn(fourthProgress);                     //progress #3, after 3rd wait
    when(mockApplicationClient.putEnterDbFreeze(FAKE_APPLICATION)).thenReturn(
        fakeProgress(DbFreezeMode.FLUSHING)/*progress #0*/);

    TaskStatus taskStatus = freezeTask.process(false);

    assertEquals(expectedStatus, taskStatus);
    verify(mockApplicationClient, times(4)).getDbFreezeProgress(FAKE_APPLICATION);
    verify(mockThreadSleeper, times(3)).sleep(anyLong());
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
    testProcess_ThreeFlushingThenEnd(fakeTransitionErrorProgress(), TaskStatus.ERROR);
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
   * Tests the case where the application does not freeze before the timeout has elapsed.
   */
  @Test
  public void testProcess_TimeoutOnThirdWait() throws InterruptedException
  {
    freezeTask.setMaxNumWaits(3);
    testProcess_ThreeFlushingThenEnd(fakeProgress(DbFreezeMode.FLUSHING), TaskStatus.ERROR);
    //Normally it would continue waiting if application is still FLUSHING
  }

}