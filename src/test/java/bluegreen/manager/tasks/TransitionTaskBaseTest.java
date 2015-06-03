package bluegreen.manager.tasks;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.mockito.Mock;
import org.mockito.Spy;

import bluegreen.manager.client.app.ApplicationClient;
import bluegreen.manager.client.app.ApplicationClientFactory;
import bluegreen.manager.client.app.ApplicationSession;
import bluegreen.manager.client.app.DbFreezeMode;
import bluegreen.manager.client.app.DbFreezeProgress;
import bluegreen.manager.model.domain.Application;
import bluegreen.manager.model.domain.EnvironmentTestHelper;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.OneEnvLoader;
import bluegreen.manager.utils.ThreadSleeper;
import bluegreen.manager.utils.WaiterParameters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
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
  private static final Integer NO_OUTER_TRY = null;
  private static final Integer OUTER_FIRST_TRY = 0;
  protected static final Application FAKE_APPLICATION = EnvironmentTestHelper.makeFakeApplication();

  @Spy
  protected WaiterParameters fakeWaiterParameters = new WaiterParameters(10L, 10L, 2, 20);

  @Mock
  protected EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  protected OneEnvLoader mockOneEnvLoader;

  @Mock
  protected ApplicationClientFactory mockApplicationClientFactory;

  @Mock
  protected ApplicationClient mockApplicationClient;

  @Mock
  protected ThreadSleeper mockThreadSleeper;

  @Mock
  protected Executor mockExecutor;

  @Mock
  protected CookieStore mockCookieStore;

  protected ApplicationSession fakeSession;

  private TransitionTestHelper transitionTestHelper = new TransitionTestHelper();

  /**
   * Common test setup.
   */
  protected void setUp(TransitionTask transitionTask)
  {
    fakeSession = new ApplicationSession(mockExecutor, mockCookieStore);
    String envName = FAKE_APPLICATION.getApplicationVm().getEnvironment().getEnvName();
    when(mockEnvLoaderFactory.createOne(envName)).thenReturn(mockOneEnvLoader);
    when(mockOneEnvLoader.getEnvironment()).thenReturn(FAKE_APPLICATION.getApplicationVm().getEnvironment());
    when(mockOneEnvLoader.getApplication()).thenReturn(FAKE_APPLICATION);
    when(mockApplicationClientFactory.create(anyString(), anyString())).thenReturn(mockApplicationClient);
    when(mockApplicationClient.authenticate(FAKE_APPLICATION)).thenReturn(fakeSession);
    transitionTask.assignTransition(1, envName);
    transitionTask.loadDataModel();
    transitionTask.initApplicationSession();
  }

  protected DbFreezeProgress fakeLockErrorProgress()
  {
    return transitionTestHelper.fakeLockErrorProgress();
  }

  protected DbFreezeProgress fakeTransitionErrorProgress(DbFreezeMode transitionErrorMode)
  {
    return transitionTestHelper.fakeTransitionErrorProgress(transitionErrorMode);
  }

  protected DbFreezeProgress fakeProgress(DbFreezeMode mode)
  {
    return transitionTestHelper.fakeProgress(mode);
  }

  /**
   * Prepares mock to return this progress after call to getDbFreezeProgress.
   */
  private void whenGetDbFreezeProgress(DbFreezeProgress progress)
  {
    when(mockApplicationClient.getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY)).thenReturn(progress);
  }

  /**
   * Verifies the mock received a call to getDbFreezeProgress.
   */
  private void verifyGetDbFreezeProgress()
  {
    verify(mockApplicationClient).getDbFreezeProgress(FAKE_APPLICATION, fakeSession, NO_OUTER_TRY);
  }

  /**
   * Tests that a null client response means the app is not ready to transition.
   */
  protected void testAppIsReadyToTransition_NullProgress(TransitionTask transitionTask)
  {
    whenGetDbFreezeProgress(null);

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertFalse(isReady);
    verifyGetDbFreezeProgress();
  }

  /**
   * Tests that a lock error response means the app is not ready to transition.
   */
  protected void testAppIsReadyToTransition_LockError(TransitionTask transitionTask)
  {
    whenGetDbFreezeProgress(fakeLockErrorProgress());

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertFalse(isReady);
    verifyGetDbFreezeProgress();
  }

  /**
   * Tests that a response in the "wrong mode" means the app is not ready to transition.
   */
  protected void testAppIsReadyToTransition_WrongMode(TransitionTask transitionTask, DbFreezeMode wrongMode)
  {
    whenGetDbFreezeProgress(fakeProgress(wrongMode));

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertFalse(isReady);
    verifyGetDbFreezeProgress();
  }

  /**
   * Tests that a response in an allowed start mode means the app is ready to transition.
   */
  protected void testAppIsReadyToTransition_AllowedStartMode(TransitionTask transitionTask,
                                                             DbFreezeMode allowedStartMode)
  {
    whenGetDbFreezeProgress(fakeProgress(allowedStartMode));

    boolean isReady = transitionTask.appIsReadyToTransition();

    assertTrue(isReady);
    verifyGetDbFreezeProgress();
  }

  /**
   * Tests that noop request doesn't invoke the applicationClient.
   */
  protected void testRequestTransition_Noop(TransitionTask transitionTask, String transitionMethodPath)
  {
    TransitionProgressChecker progressChecker = transitionTask.requestTransition(true);

    assertNull(progressChecker);
    verify(mockApplicationClient, never()).putRequestTransition(any(Application.class), any(ApplicationSession.class),
        eq(transitionMethodPath), eq(NO_OUTER_TRY));
  }

  /**
   * Prepares mock to return the given progress after putRequestTransition.
   */
  private void whenPutRequestTransition(String transitionMethodPath, DbFreezeProgress progress)
  {
    when(mockApplicationClient.putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY))
        .thenReturn(progress);
  }

  /**
   * Verifies mock received call to putRequestTransition.
   */
  private void verifyPutRequestTransition(String transitionMethodPath)
  {
    verify(mockApplicationClient).putRequestTransition(FAKE_APPLICATION, fakeSession, transitionMethodPath, OUTER_FIRST_TRY);
  }

  /**
   * Tests a null client response.
   */
  protected void testRequestTransition_NullProgress(TransitionTask transitionTask, String transitionMethodPath)
  {
    whenPutRequestTransition(transitionMethodPath, null);

    TransitionProgressChecker progressChecker = transitionTask.requestTransition(false);

    assertNull(progressChecker.getInitialProgress());
    verifyPutRequestTransition(transitionMethodPath);
  }

  /**
   * Tests a lock error response.
   */
  protected void testRequestTransition_LockError(TransitionTask transitionTask, String transitionMethodPath)
  {
    whenPutRequestTransition(transitionMethodPath, fakeLockErrorProgress());

    TransitionProgressChecker progressChecker = transitionTask.requestTransition(false);

    assertTrue(progressChecker.getInitialProgress().isLockError());
    verifyPutRequestTransition(transitionMethodPath);
  }

  /**
   * Tests a transition error response.
   */
  protected void testRequestTransition_TransitionError(TransitionTask transitionTask, String transitionMethodPath)
  {
    whenPutRequestTransition(transitionMethodPath, fakeTransitionErrorProgress(DbFreezeMode.FLUSH_ERROR));

    TransitionProgressChecker progressChecker = transitionTask.requestTransition(false);

    assertTrue(StringUtils.isNotBlank(progressChecker.getInitialProgress().getTransitionError()));
    verifyPutRequestTransition(transitionMethodPath);
  }

  /**
   * Tests that a non-error response from the client gets passed as the service return value.
   */
  protected void testRequestTransition_Normal(TransitionTask transitionTask, String transitionMethodPath)
  {
    whenPutRequestTransition(transitionMethodPath, fakeProgress(DbFreezeMode.FLUSHING));

    TransitionProgressChecker progressChecker = transitionTask.requestTransition(false);

    assertEquals(DbFreezeMode.FLUSHING, progressChecker.getInitialProgress().getMode());
    verifyPutRequestTransition(transitionMethodPath);
  }

  /**
   * Tests noop request.  ...Would be more useful to assert that there was no communication with the application,
   * but we have no mocks to prove it was really noop.
   */
  protected void testWaitForTransition_Noop(TransitionTask transitionTask, String transitionMethodPath)
  {
    assertTrue(transitionTask.waitForTransition(null, true));
  }

  /**
   * Makes a progressChecker with the given initialProgress and mocks/fakes.
   */
  private TransitionProgressChecker makeTestChecker(TransitionTask transitionTask, DbFreezeProgress initialProgress)
  {
    return new TransitionProgressChecker(transitionTask.getTransitionParameters(),
        transitionTask.context(), initialProgress, mockApplicationClient, fakeSession, FAKE_APPLICATION);
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
    TransitionProgressChecker progressChecker = makeTestChecker(transitionTask, fakeProgress(transitionalMode)/*progress #0*/);
    when(mockApplicationClient.getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt()))
        .thenReturn(fakeProgress(transitionalMode)) //progress #1, after 1st wait
        .thenReturn(fakeProgress(transitionalMode)) //progress #2, after 2nd wait
        .thenReturn(fourthProgress);                //progress #3, after 3rd wait

    boolean transitionSuccess = transitionTask.waitForTransition(progressChecker, false);

    assertEquals(expectSuccess, transitionSuccess);
    verify(mockApplicationClient, times(3)).getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt());
    verify(mockThreadSleeper, times(3)).sleep(anyLong());
  }

  /**
   * Tests the case where noop returns NOOP.
   */
  protected void testProcess_Noop(TransitionTask transitionTask, DbFreezeMode startMode)
  {
    whenGetDbFreezeProgress(fakeProgress(startMode));

    TaskStatus taskStatus = transitionTask.process(true);

    assertEquals(TaskStatus.NOOP, taskStatus);
    verifyGetDbFreezeProgress();
  }

  /**
   * Tests case where the read-only actions of a noop cause an error.
   */
  protected void testProcess_NoopError(TransitionTask transitionTask)
  {
    whenGetDbFreezeProgress(null);

    TaskStatus taskStatus = transitionTask.process(true);

    assertEquals(TaskStatus.ERROR, taskStatus);
    verifyGetDbFreezeProgress();
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
    when(mockApplicationClient.getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt()))
        .thenReturn(fakeProgress(startMode))        //initial state ...before requestTransition
        .thenReturn(fakeProgress(transitionalMode)) //progress #1, after 1st wait
        .thenReturn(fakeProgress(transitionalMode)) //progress #2, after 2nd wait
        .thenReturn(fourthProgress);                                     //progress #3, after 3rd wait
    when(mockApplicationClient.putRequestTransition(eq(FAKE_APPLICATION), eq(fakeSession), eq(transitionMethodPath), anyInt()))
        .thenReturn(fakeProgress(transitionalMode)/*progress #0*/);

    TaskStatus taskStatus = transitionTask.process(false);

    assertEquals(expectedStatus, taskStatus);
    verify(mockApplicationClient, times(4)).getDbFreezeProgress(eq(FAKE_APPLICATION), eq(fakeSession), anyInt());
    verify(mockThreadSleeper, times(3)).sleep(anyLong());
  }

}
