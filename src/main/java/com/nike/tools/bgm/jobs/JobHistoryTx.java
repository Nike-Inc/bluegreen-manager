package com.nike.tools.bgm.jobs;

import java.sql.Timestamp;
import java.util.Date;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.dao.JobHistoryDAO;
import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.model.domain.JobStatus;
import com.nike.tools.bgm.utils.NowFactory;

/**
 * Serves transactional db queries related to JobHistory.
 */
@Transactional
@Component
public class JobHistoryTx
{
  @Autowired
  private NowFactory nowFactory;

  @Autowired
  private JobHistoryDAO jobHistoryDAO;

  /**
   * Looks up the prior old job history (if recent enough).
   */
  public JobHistory findLastRelevantJobHistory(String jobName, String env1, String env2, String commandLine,
                                               boolean noop, long maxAge)
  {
    return jobHistoryDAO.findLastRelevantJobHistory(jobName, env1, env2, maxAge);
  }

  /**
   * Makes an in-progress JobHistory and saves it.
   */
  public JobHistory newJobHistoryProcessing(Job job, Date jobStartTime)
  {
    if (jobStartTime == null)
    {
      throw new IllegalArgumentException();
    }
    JobHistory newJobHistory = new JobHistory();
    newJobHistory.setStartTime(new Timestamp(nowFactory.now().getTime()));
    newJobHistory.setJobName(job.getName());
    newJobHistory.setEnv1(job.getEnv1());
    newJobHistory.setEnv2(job.getEnv2());
    newJobHistory.setCommandLine(job.getCommandLine());
    newJobHistory.setStatus(JobStatus.PROCESSING);
    jobHistoryDAO.persist(newJobHistory);
    return newJobHistory;
  }

  /**
   * Closes a jobHistory with an endTime and new status, then merges to persistence context.
   * <p/>
   * jobHistory could be detached or managed.
   */
  public void closeJobHistory(JobHistory jobHistory, JobStatus jobStatus)
  {
    assertNotNew(jobHistory);
    jobHistory.setEndTime(new Timestamp(nowFactory.now().getTime()));
    jobHistory.setStatus(jobStatus);
    jobHistoryDAO.merge(jobHistory);
  }

  /**
   * Throws if the jobHistory is a new entity.
   */
  private void assertNotNew(JobHistory jobHistory)
  {
    if (jobHistory.getId() == 0)
    {
      throw new IllegalArgumentException("Expected detached jobHistory but received new: " + jobHistory);
    }
  }
}
