package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.jobs.JobFakery;
import com.nike.tools.bgm.model.dao.TaskHistoryDAO;
import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.NowFactory;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability to perform transactions of task history.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskHistoryTxTest
{
  @InjectMocks
  private TaskHistoryTx taskHistoryTx;

  @Mock
  private NowFactory mockNowFactory;

  @Mock
  private TaskHistoryDAO mockTaskHistoryDAO;

  private TaskFakery taskFakery = new TaskFakery();
  private JobFakery jobFakery = new JobFakery(taskFakery);

  @Before
  public void setUp()
  {
    when(mockNowFactory.now()).thenReturn(START_TIME);
  }

  /**
   * Tests the ability to setup and save a new task history for a properly processing task.
   */
  @Test
  public void testNewTaskHistoryProcessing()
  {
    JobHistory jobHistory = jobFakery.makeFakeJobHistory(null /*no prior task histories*/);
    Task task = taskFakery.makeFakeTask(0);
    TaskHistory newTaskHistory = taskHistoryTx.newTaskHistoryProcessing(task, START_TIME, jobHistory);

    verify(mockTaskHistoryDAO).save(newTaskHistory);
    assertEquals(newTaskHistory.getStatus(), TaskStatus.PROCESSING);
    assertNull(newTaskHistory.getEndTime());
  }

  /**
   * Tests the ability to setup and save a new task history for a skipped task.
   */
  @Test
  public void testNewTaskHistorySkipped()
  {
    JobHistory jobHistory = jobFakery.makeFakeJobHistory(null /*no prior task histories*/);
    Task task = taskFakery.makeFakeTask(0);
    TaskHistory newTaskHistory = taskHistoryTx.newTaskHistorySkipped(task, START_TIME, jobHistory);

    verify(mockTaskHistoryDAO).save(newTaskHistory);
    verify(mockNowFactory).now();
    assertEquals(newTaskHistory.getStatus(), TaskStatus.SKIPPED);
    assertNotNull(newTaskHistory.getEndTime());
  }

  /**
   * Tests the ability to persistently update an existing task history whose work is done.
   */
  @Test
  public void testCloseTaskHistory()
  {
    JobHistory jobHistory = jobFakery.makeFakeJobHistory(new TaskStatus[] { TaskStatus.PROCESSING });
    TaskHistory taskHistory = jobHistory.getTaskHistories().get(0);

    taskHistoryTx.closeTaskHistory(taskHistory, TaskStatus.DONE);

    verify(mockTaskHistoryDAO).refresh(taskHistory);
    verify(mockTaskHistoryDAO).save(taskHistory);
    verify(mockNowFactory).now();
    assertEquals(taskHistory.getStatus(), TaskStatus.DONE);
    assertNotNull(taskHistory.getEndTime());
  }
}