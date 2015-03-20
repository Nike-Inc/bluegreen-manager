package com.nike.tools.bgm.model.dao;

import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.utils.NowFactory;

/**
 * Data access object for JobHistory.
 */
@Repository
public class JobHistoryDAO extends GenericDAO<JobHistory>
{
  @Autowired
  NowFactory nowFactory;

  /**
   * Finds the most recently started job history record matching the given jobName/env1/env2 which is no older
   * than maxAge.  Null if none found.
   */
  public JobHistory findLastRelevantJobHistory(String jobName, String env1, String env2, long maxAge)
  {
    String queryString = "SELECT jh FROM " + JobHistory.class.getSimpleName() + " jh WHERE "
        + "jh.jobName = '" + jobName + "' "
        + "AND jh.env1 = '" + env1 + "' "
        + "AND " + makeNullableCondition("jh.env2", env2) + " "
        + "AND jh.startTime > :oldestAllowedStartTime "
        + "ORDER BY jh.startTime DESC ";
    Query query = entityManager.createQuery(queryString);
    query.setParameter("oldestAllowedStartTime", makeTimestampBeforeNow(maxAge));
    query.setMaxResults(1);
    List<JobHistory> results = query.getResultList();
    if (results != null && results.size() > 0)
    {
      return results.get(0);
    }
    else
    {
      return null;
    }
  }

  /**
   * Returns a short condition string to be used in a WHERE clause, essentially "where fieldName = filterValue"
   * but allowing for null.
   */
  private String makeNullableCondition(String fieldName, String filterValue)
  {
    if (filterValue == null)
    {
      return fieldName + " IS NULL";
    }
    else
    {
      return fieldName + " = '" + filterValue + "'";
    }
  }

  /**
   * Returns a Timestamp set to NOW minus the specified milliseconds.
   */
  private Timestamp makeTimestampBeforeNow(long millisecondsAgo)
  {
    return new Timestamp(nowFactory.now().getTime() - millisecondsAgo);
  }
}
