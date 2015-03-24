package com.nike.tools.bgm.model.dao;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.JobHistory;
import com.nike.tools.bgm.utils.NowFactory;

import static com.nike.tools.bgm.utils.TimeFakery.START_TIME;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bare minimum test, since the entityManager is mocked.
 */
@RunWith(MockitoJUnitRunner.class)
public class JobHistoryDAOTest
{
  public static String JOB_NAME = "theJob";
  public static String ENV_NAME1 = "env1";
  public static String ENV_NAME2 = "env2";
  public static long MAX_AGE = 10000L;

  @InjectMocks
  private JobHistoryDAO jobHistoryDAO;

  @Mock
  private EntityManager mockEntityManager;

  @Mock
  private Query mockQuery;

  @Mock
  private NowFactory mockNowFactory;

  @Before
  public void setUp()
  {
    when(mockEntityManager.createQuery(anyString())).thenReturn(mockQuery);
    when(mockNowFactory.now()).thenReturn(START_TIME);
  }

  @Test
  public void testFindLastRelevantJobHistory()
  {
    JobHistory jobHistory = jobHistoryDAO.findLastRelevantJobHistory(JOB_NAME, ENV_NAME1, ENV_NAME2, MAX_AGE);

    verify(mockEntityManager).createQuery(argThat(is(allOf(containsString(JOB_NAME),
        containsString(ENV_NAME1), containsString(ENV_NAME2)))));
  }
}