package com.nike.tools.bgm.tasks;

import org.apache.http.Header;
import org.apache.http.client.fluent.Executor;
import org.mockito.Mock;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeProgress;
import com.nike.tools.bgm.env.EnvironmentTx;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mocks and methods shared by freeze test and thaw test.
 */
public abstract class TransitionTaskBaseTest
{
  private static final String PROGRESS_USER = "someUser";
  private static final Integer NO_OUTER_TRY = null;
  private static final Integer OUTER_FIRST_TRY = 0;
  protected static final Application FAKE_APPLICATION = ApplicationTestHelper.makeFakeApplication();

  @Mock
  protected ApplicationClient mockApplicationClient;

  @Mock
  protected ThreadSleeper mockThreadSleeper;

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Mock
  protected Executor mockExecutor;

  @Mock
  protected Header mockCookieHeader;

  protected ApplicationSession fakeSession;

  /**
   * Common test setup.
   */
  protected void setUp(TransitionTask transitionTask)
  {
    fakeSession = new ApplicationSession(mockExecutor, mockCookieHeader);
    String envName = FAKE_APPLICATION.getApplicationVm().getEnvironment().getEnvName();
    when(mockEnvironmentTx.findNamedEnv(envName)).thenReturn(FAKE_APPLICATION.getApplicationVm().getEnvironment());
    when(mockApplicationClient.authenticate(FAKE_APPLICATION)).thenReturn(fakeSession);
    transitionTask.initTransition(1, envName);
    transitionTask.initApplicationSession();
  }

  /**
   * Makes a fake progress object showing a lock error.
   */
  protected DbFreezeProgress fakeLockErrorProgress()
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setLockError(true);
    return progress;
  }

  /**
   * Makes a fake progress object showing a transition error.
   */
  protected DbFreezeProgress fakeTransitionErrorProgress(DbFreezeMode transitionErrorMode)
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
  protected DbFreezeProgress fakeProgress(DbFreezeMode mode)
  {
    DbFreezeProgress progress = new DbFreezeProgress();
    progress.setMode(mode);
    progress.setUsername(PROGRESS_USER);
    progress.setStartTime(START_TIME_STRING);
    progress.setEndTime(START_TIME_STRING);
    return progress;
  }

  /**
   * Tests that a null client response means the app is not ready to transition.
   */
  protected void testAppIsReadyToTransition_NullProgress(TransitionTask transitionTask)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY)).thenReturn(null);

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertFalse(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests that a lock error response means the app is not ready to transition.
   */
  protected void testAppIsReadyToTransition_LockError(TransitionTask transitionTask)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY))
        .thenReturn(fakeLockErrorProgress());

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertFalse(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests that a response in the "wrong mode" means the app is not ready to transition.
   */
  protected void testAppIsReadyToTransition_WrongMode(TransitionTask transitionTask, DbFreezeMode wrongMode)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY))
        .thenReturn(fakeProgress(wrongMode));

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertFalse(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests that a response in an allowed start mode means the app is ready to transition.
   */
  protected void testAppIsReadyToTransition_AllowedStartMode(TransitionTask transitionTask,
                                                             DbFreezeMode allowedStartMode)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY))
        .thenReturn(fakeProgress(allowedStartMode));

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertTrue(isReady);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  protected void testRequestTransition_Noop(TransitionTask transitionTask, String transitionMethodPath)
  {
    DbFreezeProgress progress = transitionTask.requestTransition(true);

    assertNull(progress);
    verify(mockApplicationClient, never()).putRequestTransition(any(Application.class), any(ApplicationSession.class),
        eq(transitionMethodPath), eq(NO_OUTER_TRY));
  }

  /**
   * Tests a null client response.
   */
  protected void testRequestTransition_NullProgress(TransitionTask transitionTask, String transitionMethodPath)
  {
    when(mockApplicationClient.putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY))
        .thenReturn(null);

    DbFreezeProgress progress = transitionTask.requestTransition(false);

    assertNull(progress);
    verify(mockApplicationClient).putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY);
  }

  /**
   * Tests a lock error response.
   */
  protected void testRequestTransition_LockError(TransitionTask transitionTask, String transitionMethodPath)
  {
    when(mockApplicationClient.putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY))
        .thenReturn(null);

    DbFreezeProgress progress = transitionTask.requestTransition(false);

    assertNull(progress);
    verify(mockApplicationClient).putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY);
  }

  /**
   * Tests a transition error response.
   */
  protected void testRequestTransition_TransitionError(TransitionTask transitionTask, String transitionMethodPath)
  {
    when(mockApplicationClient.putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY))
        .thenReturn(fakeTransitionErrorProgress(DbFreezeMode.FLUSH_ERROR));

    DbFreezeProgress progress = transitionTask.requestTransition(false);

    assertNull(progress);
    verify(mockApplicationClient).putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY);
  }

  /**
   * Tests that a non-error response from the client gets passed as the service return value.
   */
  protected void testRequestTransition_Normal(TransitionTask transitionTask, String transitionMethodPath)
  {
    when(mockApplicationClient.putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY))
        .thenReturn(fakeProgress(DbFreezeMode.FLUSHING));

    DbFreezeProgress progress = transitionTask.requestTransition(false);

    assertNotNull(progress);
    verify(mockApplicationClient).putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY);
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  protected void testWaitForTransition_Noop(TransitionTask transitionTask, String transitionMethodPath)
  {
    boolean ok = transitionTask.waitForTransition(true, null);

    assertTrue(ok);
    verify(mockApplicationClient, never()).putRequestTransition(any(Application.class), any(ApplicationSession.class),
        eq(transitionMethodPath), eq(OUTER_FIRST_TRY));
  }

  /**
   * Tests the case where we get the transitional response three times, followed by a fourth progress object that
   * ends the waiting.
   */
  protected void testWaitForTransition_ThreeFlushingThenEnd(TransitionTask transitionTask,
                                                            DbFreezeMode transitionalMode,
                                                            DbFreezeProgress fourthProgress,
                                                            boolean expectSuccess) throws InterruptedException
  {
    transitionTask.setWaitReportInterval(2); //Just to see the extra logging on progress object #2, can't assert it though
    when(mockApplicationClient.getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt()))
        .thenReturn(fakeProgress(transitionalMode)) //progress #1, after 1st wait
        .thenReturn(fakeProgress(transitionalMode)) //progress #2, after 2nd wait
        .thenReturn(fourthProgress);                //progress #3, after 3rd wait

    boolean transitionSuccess = transitionTask.waitForTransition(false, fakeProgress(transitionalMode)/*progress #0*/);

    assertEquals(expectSuccess, transitionSuccess);
    verify(mockApplicationClient, times(3)).getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt());
    verify(mockThreadSleeper, times(3)).sleep(anyLong());
  }

  /**
   * Tests the case where noop returns NOOP.
   */
  protected void testProcess_Noop(TransitionTask transitionTask, DbFreezeMode startMode)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY))
        .thenReturn(fakeProgress(startMode));

    TaskStatus taskStatus = transitionTask.process(true);

    assertEquals(TaskStatus.NOOP, taskStatus);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests case where the read-only actions of a noop cause an error.
   */
  protected void testProcess_NoopError(TransitionTask transitionTask)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY))
        .thenReturn(null);

    TaskStatus taskStatus = transitionTask.process(true);

    assertEquals(TaskStatus.ERROR, taskStatus);
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests the case where we get a transitional response three times, followed by a fourth progress object that
   * ends the waiting.  (Technically there are five total progress objects because initial state is also "progress.")
   */
  protected void testProcess_ThreeFlushingThenEnd(TransitionTask transitionTask,
                                                  DbFreezeMode startMode,
                                                  DbFreezeMode transitionalMode,
                                                  String transitionMethodPath,
                                                  DbFreezeProgress fourthProgress,
                                                  TaskStatus expectedStatus) throws InterruptedException
  {
    transitionTask.setWaitReportInterval(2); //Just to see the extra logging on progress object #2, can't assert it though
    when(mockApplicationClient.getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt()))
        .thenReturn(fakeProgress(startMode))        //initial state ...before requestTransition
        .thenReturn(fakeProgress(transitionalMode)) //progress #1, after 1st wait
        .thenReturn(fakeProgress(transitionalMode)) //progress #2, after 2nd wait
        .thenReturn(fourthProgress);                //progress #3, after 3rd wait
    when(mockApplicationClient.putRequestTransition(eq(FAKE_APPLICATION), eq(fakeSession), eq(transitionMethodPath), anyInt()))
        .thenReturn(fakeProgress(transitionalMode)/*progress #0*/);

    TaskStatus taskStatus = transitionTask.process(false);

    assertEquals(expectedStatus, taskStatus);
    verify(mockApplicationClient, times(4)).getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt());
    verify(mockThreadSleeper, times(3)).sleep(anyLong());
  }

}
