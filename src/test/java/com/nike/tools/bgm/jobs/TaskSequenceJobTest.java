package com.nike.tools.bgm.jobs;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.JobStatus;
import com.nike.tools.bgm.model.domain.TaskHistory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.model.tx.JobHistoryTx;
import com.nike.tools.bgm.tasks.Task;
import com.nike.tools.bgm.tasks.TaskFakery;
import com.nike.tools.bgm.tasks.TaskRun;
import com.nike.tools.bgm.tasks.TaskRunProcessor;
import com.nike.tools.bgm.utils.NowFactory;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the the abstract TaskSequenceJob, which is the base class of all concrete jobs.
 * <p/>
 * Because of the mocking, the printed "JOB SUMMARY" reports some nulls.
 * <p/>
 * There is no need to test skip/force cases here, that is done in TaskRunProcessorTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskSequenceJobTest
{
  private static final int NUM_FAKE_TASKS = 2;
  private static final String ENV_NAME1 = "env1";
  private static final String ENV_NAME2 = "env2";
  private TaskFakery taskFakery = new TaskFakery();
  private JobFakery jobFakery = new JobFakery(taskFakery);

  @InjectMocks
  private TaskSequenceJob jobNormal = new TaskSequenceJobTestImpl("normal cmdline", false, false,
      jobFakery.makeFakeJobHistory(null), taskFakery, ENV_NAME1, ENV_NAME2);

  @InjectMocks
  private TaskSequenceJob jobNoop = new TaskSequenceJobTestImpl("noop cmdline", true, false,
      jobFakery.makeFakeJobHistory(null), taskFakery, ENV_NAME1, ENV_NAME2);

  @Mock
  private NowFactory mockNowFactory;

  @Mock
  private JobHistoryTx mockJobHistoryTx;

  @Mock
  private TaskRunProcessor mockTaskRunProcessor;

  private JobHistory newJobHistory = new JobHistory();

  @Before
  public void setUp()
  {
    when(mockNowFactory.now()).thenReturn(START_TIME);
    when(mockJobHistoryTx.newJobHistoryProcessing(jobNormal, START_TIME)).thenReturn(newJobHistory);
  }

  /**
   * Adds a fake task history (unless noop) and returns the specified task status.
   */
  private Answer<TaskStatus> addFakeTaskHistory(final TaskStatus taskStatus)
  {
    return new Answer<TaskStatus>()
    {
      @Override
      public TaskStatus answer(InvocationOnMock invocation) throws Throwable
      {
        Object[] args = invocation.getArguments();
        TaskRun taskRun = (TaskRun) args[0];
        JobHistory newJobHistory = taskRun.getNewJobHistory();
        if (newJobHistory != null && newJobHistory.getTaskHistories() != null) //else noop
        {
          taskFakery.addOneFakeTaskHistory(newJobHistory.getTaskHistories().size(), taskStatus, newJobHistory);
        }
        return taskStatus;
      }
    };
  }

  /**
   * Tests the ability of the abstract TaskSequenceJob to process a list of fake tasks.
   * The normal case, with some tasks that run without force/skip/noop and complete successfully.
   */
  @Test
  public void testProcess_Normal()
  {
    newJobHistory.setTaskHistories(new ArrayList<TaskHistory>());
    when(mockTaskRunProcessor.attemptTask(any(TaskRun.class))).then(addFakeTaskHistory(TaskStatus.DONE));

    jobNormal.process();

    InOrder inorder = inOrder(mockJobHistoryTx, mockTaskRunProcessor);
    inorder.verify(mockJobHistoryTx).newJobHistoryProcessing(jobNormal, START_TIME);
    inorder.verify(mockTaskRunProcessor, times(NUM_FAKE_TASKS)).attemptTask(any(TaskRun.class));
    inorder.verify(mockJobHistoryTx).closeJobHistory(any(JobHistory.class), eq(JobStatus.DONE));
  }

  /**
   * Tests the ability of the abstract TaskSequenceJob to process a list of fake tasks.
   * The noop case, where tasks don't persist anything.
   */
  @Test
  public void testProcess_Noop()
  {
    newJobHistory.setTaskHistories(new ArrayList<TaskHistory>());
    when(mockTaskRunProcessor.attemptTask(any(TaskRun.class))).then(addFakeTaskHistory(TaskStatus.NOOP));

    jobNoop.process();

    verifyZeroInteractions(mockJobHistoryTx);
    verify(mockTaskRunProcessor, times(NUM_FAKE_TASKS)).attemptTask(any(TaskRun.class));
  }

  /**
   * Implements the abstract TaskSequenceJob with a list of fake tasks.
   */
  private static class TaskSequenceJobTestImpl extends TaskSequenceJob
  {
    private String env1;
    private String env2;

    protected TaskSequenceJobTestImpl(String commandLine,
                                      boolean noop,
                                      boolean force,
                                      JobHistory oldJobHistory,
                                      TaskFakery taskFakery,
                                      String env1,
                                      String env2)
    {
      super(commandLine, noop, force, oldJobHistory);
      List<Task> tasks = new ArrayList<Task>();
      for (int idx = 0; idx < NUM_FAKE_TASKS; ++idx)
      {
        tasks.add(taskFakery.makeFakeTask(idx));
      }
      this.tasks = tasks;
    }

    @Override
    public String getEnv1()
    {
      return env1;
    }

    @Override
    public String getEnv2()
    {
      return env2;
    }
  }
}