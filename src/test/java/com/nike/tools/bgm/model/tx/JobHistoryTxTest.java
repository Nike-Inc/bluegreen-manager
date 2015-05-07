package com.nike.tools.bgm.model.tx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.jobs.Job;
import com.nike.tools.bgm.jobs.JobFakery;
import com.nike.tools.bgm.model.dao.JobHistoryDAO;
import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.JobStatus;
import com.nike.tools.bgm.tasks.TaskFakery;
import com.nike.tools.bgm.utils.NowFactory;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability to perform transactions of job history.
 */
@RunWith(MockitoJUnitRunner.class)
public class JobHistoryTxTest
{
  public static final long JOB_ID = 1L;

  @InjectMocks
  private JobHistoryTx jobHistoryTx;

  @Mock
  private NowFactory mockNowFactory;

  @Mock
  private JobHistoryDAO mockJobHistoryDAO;

  private TaskFakery taskFakery = new TaskFakery();
  private JobFakery jobFakery = new JobFakery(taskFakery);

  @Before
  public void setUp()
  {
    when(mockNowFactory.now()).thenReturn(START_TIME);
  }

  /**
   * Tests the ability to setup and save a new job history for a properly processing job.
   */
  @Test
  public void testNewJobHistoryProcessing()
  {
    Job job = jobFakery.makeFakeJob();
    JobHistory newJobHistory = jobHistoryTx.newJobHistoryProcessing(job, START_TIME);

    verify(mockJobHistoryDAO).persist(newJobHistory);
    assertEquals(newJobHistory.getStatus(), JobStatus.PROCESSING);
    assertNull(newJobHistory.getEndTime());
  }

  /**
   * Tests the ability to persistently update an existing job history whose work is done.
   */
  @Test
  public void testCloseJobHistory()
  {
    JobHistory jobHistory = jobFakery.makeFakeJobHistory(null /*no task histories required*/);
    jobHistory.setId(JOB_ID);

    jobHistoryTx.closeJobHistory(jobHistory, JobStatus.DONE);

    verify(mockJobHistoryDAO).merge(jobHistory);
    verify(mockNowFactory).now();
    assertEquals(jobHistory.getStatus(), JobStatus.DONE);
    assertNotNull(jobHistory.getEndTime());
  }
}
