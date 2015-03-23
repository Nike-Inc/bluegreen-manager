package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.jobs.JobFakery;
import com.nike.tools.bgm.jobs.SkipRemarkHelper;
import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the private and public bits of the taskRun processor.  Such as: persistence of task histories,
 * the choice of whether to skip or process a task.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskRunProcessorTest
{
  @InjectMocks
  private TaskRunProcessor taskRunProcessor;

  @Spy
  private SkipRemarkHelper mockSkipRemarkHelper;

  @Mock
  private TaskHistoryTx mockTaskHistoryTx;

  private TaskFakery taskFakery = new TaskFakery();
  private JobFakery jobFakery = new JobFakery(taskFakery);

  /**
   * Tests that the processor can find prior task status associated to a job history.
   */
  @Test
  public void testFindPriorTaskHistory()
  {
    TaskStatus[] priorTaskStatus = new TaskStatus[] { TaskStatus.SKIPPED, TaskStatus.DONE };
    JobHistory oldJobHistory = jobFakery.makeFakeJobHistory(priorTaskStatus);
    for (int idx = 0; idx < priorTaskStatus.length; ++idx)
    {
      Task task = taskFakery.makeFakeTask(idx);
      TaskHistory priorTaskHistory = taskRunProcessor.findPriorTaskHistory(task, oldJobHistory);
      assertEquals(priorTaskHistory.getTaskName(), task.getName());
      assertEquals(priorTaskHistory.getStatus(), priorTaskStatus[idx]);
    }
  }

  /**
   * Tests that the processor will skip or not skip based on a non-null prior task status.
   * <p/>
   * Given: force=false.
   */
  @Test
  public void testChooseToSkip_NoForce()
  {
    TaskStatus[] priorTaskStatus = new TaskStatus[] { TaskStatus.SKIPPED, TaskStatus.DONE, TaskStatus.PROCESSING, TaskStatus.ERROR };
    boolean[] expectSkip = new boolean[] { true, true, false, false };
    JobHistory oldJobHistory = jobFakery.makeFakeJobHistory(priorTaskStatus);
    for (int idx = 0; idx < priorTaskStatus.length; ++idx)
    {
      Task task = taskFakery.makeFakeTask(idx);
      testChooseToSkipOrForce(task, false/*force*/, oldJobHistory, expectSkip[idx]);
    }
  }

  /**
   * Tests that the processor never skips given force=true.
   * Testcase involves a non-null prior task status.
   */
  @Test
  public void testChooseToSkip_YesForce()
  {
    TaskStatus[] priorTaskStatus = new TaskStatus[] { TaskStatus.SKIPPED, TaskStatus.DONE, TaskStatus.PROCESSING, TaskStatus.ERROR };
    JobHistory oldJobHistory = jobFakery.makeFakeJobHistory(priorTaskStatus);
    for (int idx = 0; idx < priorTaskStatus.length; ++idx)
    {
      Task task = taskFakery.makeFakeTask(idx);
      testChooseToSkipOrForce(task, true/*force*/, oldJobHistory, false/*expectSkip*/);
    }
  }

  /**
   * Tests that the processor never skips in the absence of relevant prior task history.
   * Regardless of force.
   * <p/>
   * force=false is the better testcase here.
   */
  @Test
  public void testChooseToSkip_NoHistory()
  {
    JobHistory oldJobHistory = jobFakery.makeFakeJobHistory(null/*no task history*/);
    for (int idx = 0; idx < 4; ++idx)
    {
      Task task = taskFakery.makeFakeTask(idx);
      testChooseToSkipOrForce(task, false/*force*/, oldJobHistory, false/*expectSkip*/);
    }
  }

  /**
   * Given a particular task and old job history, tests that the processor knows whether to skip or force.
   */
  private void testChooseToSkipOrForce(Task task, boolean force,
                                       JobHistory oldJobHistory, boolean expectSkip)
  {
    boolean noop = false;
    JobHistory newJobHistory = null;
    TaskRun taskRun = new TaskRun(task, noop, force, START_TIME, newJobHistory, oldJobHistory);
    boolean skip = taskRunProcessor.chooseToSkipOrForce(taskRun);
    assertEquals(expectSkip, skip);
  }

  /**
   * Tests the ability to open/close task history and process a task (after it has already been determined that
   * the task will not be skipped).
   * <p/>
   * Specifically the case of a noop task.  Should not perform any task db transactions.
   */
  @Test
  public void testOpenProcessCloseTask_Noop()
  {
    boolean noop = true;
    TaskStatus expectedStatus = TaskStatus.NOOP;
    Task mockTask = mock(Task.class);
    when(mockTask.process(noop)).thenReturn(expectedStatus);

    TaskStatus taskStatus = testOpenProcessCloseTask(noop, mockTask, null);

    assertEquals(expectedStatus, taskStatus);
    verifyZeroInteractions(mockTaskHistoryTx);
    verify(mockTask).process(noop);
  }

  /**
   * Tests the ability to open/close task history and process a task (after it has already been determined that
   * the task will not be skipped).
   * <p/>
   * Specifically the case where the task ends successfully (done).
   */
  @Test
  public void testOpenProcessCloseTask_Done()
  {
    boolean noop = false;
    TaskStatus expectedStatus = TaskStatus.DONE;
    Task mockTask = mock(Task.class);
    when(mockTask.process(noop)).thenReturn(expectedStatus);
    TaskHistory mockTaskHistory = mock(TaskHistory.class);

    TaskStatus taskStatus = testOpenProcessCloseTask(noop, mockTask, mockTaskHistory);

    assertEquals(expectedStatus, taskStatus);
    verify(mockTaskHistoryTx).newTaskHistoryProcessing(mockTask, START_TIME, null);
    verify(mockTask).process(noop);
    verify(mockTaskHistoryTx).closeTaskHistory(mockTaskHistory, expectedStatus);
  }

  /**
   * Tests the ability to open/close task history and process a task (after it has already been determined that
   * the task will not be skipped).
   * <p/>
   * Specifically the case where the task throws and ends in error state.
   */
  @Test
  public void testOpenProcessCloseTask_Throw()
  {
    boolean noop = false;
    Task mockTask = mock(Task.class);
    when(mockTask.process(noop)).thenThrow(RuntimeException.class);
    TaskHistory mockTaskHistory = mock(TaskHistory.class);
    when(mockTaskHistoryTx.newTaskHistoryProcessing(mockTask, START_TIME, null)).thenReturn(mockTaskHistory);

    boolean caught = false;
    TaskStatus taskStatus = null;

    try
    {
      taskStatus = testOpenProcessCloseTask(noop, mockTask, mockTaskHistory);
    }
    catch (RuntimeException e)
    {
      caught = true;
    }

    assertTrue(caught);
    assertNull(taskStatus);
    verify(mockTaskHistoryTx).newTaskHistoryProcessing(mockTask, START_TIME, null);
    verify(mockTask).process(noop);
    verify(mockTaskHistoryTx).closeTaskHistory(mockTaskHistory, TaskStatus.ERROR);
  }

  /**
   * Sets up the test and runs the target method, but does not perform verification.
   */
  private TaskStatus testOpenProcessCloseTask(boolean noop, Task mockTask, TaskHistory mockTaskHistory)
  {
    boolean force = false;
    JobHistory newJobHistory = null;
    JobHistory oldJobHistory = null;
    TaskRun taskRun = new TaskRun(mockTask, noop, force, START_TIME, newJobHistory, oldJobHistory);
    if (mockTaskHistory != null)
    {
      when(mockTaskHistoryTx.newTaskHistoryProcessing(mockTask, START_TIME, newJobHistory)).thenReturn(mockTaskHistory);
    }

    return taskRunProcessor.openProcessCloseTask(taskRun);
  }

  /**
   * Tests the ability to persist "skipped task history".
   */
  @Test
  public void testSkipTaskHistory()
  {
    Task task = taskFakery.makeFakeTask(0);
    boolean noop = false;
    boolean force = false;
    JobHistory newJobHistory = null;
    JobHistory oldJobHistory = null;
    TaskRun taskRun = new TaskRun(task, noop, force, START_TIME, newJobHistory, oldJobHistory);

    TaskStatus taskStatus = taskRunProcessor.skipTaskHistory(taskRun);

    assertEquals(taskStatus, TaskStatus.SKIPPED);
    verify(mockTaskHistoryTx).newTaskHistorySkipped(task, START_TIME, newJobHistory);
  }
}
