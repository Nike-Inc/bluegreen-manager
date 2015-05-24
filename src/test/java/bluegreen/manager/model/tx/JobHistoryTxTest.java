package bluegreen.manager.model.tx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.jobs.Job;
import bluegreen.manager.jobs.JobFakery;
import bluegreen.manager.model.dao.JobHistoryDAO;
import bluegreen.manager.model.domain.JobHistory;
import bluegreen.manager.model.domain.JobStatus;
import bluegreen.manager.tasks.TaskFakery;
import bluegreen.manager.utils.NowFactory;
import static bluegreen.manager.utils.TimeFakery.START_TIME;
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
