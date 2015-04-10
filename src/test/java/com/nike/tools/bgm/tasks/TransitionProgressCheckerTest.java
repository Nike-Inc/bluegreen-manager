package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.client.app.DbFreezeMode;
import com.nike.tools.bgm.client.app.DbFreezeProgress;
import com.nike.tools.bgm.model.domain.Application;

import static com.nike.tools.bgm.tasks.TransitionTestHelper.TRANSITION_PARAMETERS;
import static com.nike.tools.bgm.tasks.TransitionTestHelper.VERB;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransitionProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context)";
  private static final int WAIT_NUM = 1;

  @Mock
  private ApplicationClient mockApplicationClient;

  @Mock
  private ApplicationSession mockApplicationSession;

  @Mock
  private Application mockApplication;

  private TransitionTestHelper transitionTestHelper = new TransitionTestHelper();

  /*
  In these examples we're using TRANSITION_PARAMETERS based on the Frozen -> Thaw -> Normal transition.
   */

  private TransitionProgressChecker makeProgressChecker(DbFreezeProgress progress)
  {
    return new TransitionProgressChecker(TRANSITION_PARAMETERS, LOG_CONTEXT, progress,
        mockApplicationClient, mockApplicationSession, mockApplication);
  }

  private DbFreezeProgress fakeLockErrorProgress()
  {
    return transitionTestHelper.fakeLockErrorProgress();
  }

  private DbFreezeProgress fakeTransitionErrorProgress(DbFreezeMode transitionErrorMode)
  {
    return transitionTestHelper.fakeTransitionErrorProgress(transitionErrorMode);
  }

  private DbFreezeProgress fakeProgress(DbFreezeMode mode)
  {
    return transitionTestHelper.fakeProgress(mode);
  }

  @Test
  public void testGetDescription()
  {
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.THAW));
    assertTrue(progressChecker.getDescription().contains(VERB));
  }

  /**
   * Initial progress = transitional.  Not done.
   */
  @Test
  public void testInitialCheck_Transitional()
  {
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.THAW));
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
  }

  /**
   * Initial progress = destination.  Done.
   */
  @Test
  public void testInitialCheck_Destination()
  {
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.NORMAL));
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertTrue(progressChecker.getResult());
  }

  /**
   * Expect the initial progress to result in "done, bad result".
   */
  private void testInitialCheck_DoneBadResult(TransitionProgressChecker progressChecker)
  {
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertFalse(progressChecker.getResult());
  }

  /**
   * Initial progress = lock error.  Done, bad result.
   */
  @Test
  public void testInitialCheck_LockError()
  {
    testInitialCheck_DoneBadResult(makeProgressChecker(fakeLockErrorProgress()));
  }

  /**
   * Initial progress = transition error.  Done, bad result.
   */
  @Test
  public void testInitialCheck_TransitionError()
  {
    testInitialCheck_DoneBadResult(makeProgressChecker(fakeTransitionErrorProgress(DbFreezeMode.THAW_ERROR)));
  }

  /**
   * Initial progress = null.  Done, bad result.
   */
  @Test
  public void testInitialCheck_Null()
  {
    testInitialCheck_DoneBadResult(makeProgressChecker(null));
  }

  /**
   * Prepares mock response to getDbFreezeProgress.
   */
  private void whenGetDbFreezeProgress(DbFreezeProgress progress)
  {
    when(mockApplicationClient.getDbFreezeProgress(mockApplication, mockApplicationSession, WAIT_NUM))
        .thenReturn(progress);
  }

  /**
   * Followup progress = transitional.  Not done.
   */
  @Test
  public void testFollowupCheck_Transitional()
  {
    whenGetDbFreezeProgress(fakeProgress(DbFreezeMode.THAW));
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.THAW));
    progressChecker.followupCheck(WAIT_NUM);
    assertFalse(progressChecker.isDone());
  }

  /**
   * Followup progress = destination.  Done.
   */
  @Test
  public void testFollowupCheck_Destination()
  {
    whenGetDbFreezeProgress(fakeProgress(DbFreezeMode.NORMAL));
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.THAW));
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertTrue(progressChecker.getResult());
  }

  /**
   * Expect the followup progress to result in "done, bad result".
   * <p/>
   * Always start with initialProgress=THAW.
   */
  private void testFollowupCheck_DoneBadResult(DbFreezeProgress fakeProgress)
  {
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.THAW));
    whenGetDbFreezeProgress(fakeProgress);
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertFalse(progressChecker.getResult());
  }

  /**
   * Followup progress = lock error.  Done, bad result.
   */
  @Test
  public void testFollowupCheck_LockError()
  {
    testFollowupCheck_DoneBadResult(fakeLockErrorProgress());
  }

  /**
   * Followup progress = transition error.  Done, bad result.
   */
  @Test
  public void testFollowupCheck_TransitionError()
  {
    testFollowupCheck_DoneBadResult(fakeTransitionErrorProgress(DbFreezeMode.THAW_ERROR));
  }

  /**
   * Followup progress = null.  Done, bad result.
   */
  @Test
  public void testFollowupCheck_Null()
  {
    testFollowupCheck_DoneBadResult(null);
  }

  @Test
  public void testTimeout()
  {
    TransitionProgressChecker progressChecker = makeProgressChecker(fakeProgress(DbFreezeMode.THAW));
    assertFalse(progressChecker.timeout());
  }
}