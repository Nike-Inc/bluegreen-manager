package com.nike.tools.bgm.model.dao;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.domain.Environment;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bare minimum test, since the entityManager is mocked.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentDAOTest
{
  public static String ENV_NAME1 = "env1";
  public static String ENV_NAME2 = "env2";

  @InjectMocks
  private EnvironmentDAO environmentDAO;

  @Mock
  private EntityManager mockEntityManager;

  @Mock
  private Query mockQuery;

  @Before
  public void setUp()
  {
    when(mockEntityManager.createQuery(anyString())).thenReturn(mockQuery);
  }

  @Test
  public void testFindNamedEnv()
  {
    Environment env = environmentDAO.findNamedEnv(ENV_NAME1);

    verify(mockEntityManager).createQuery(contains("SELECT"));
  }

  @Test
  public void testFindNamedEnvs()
  {
    List<Environment> envs = environmentDAO.findNamedEnvs(ENV_NAME1, ENV_NAME2);

    verify(mockEntityManager).createQuery(matches("SELECT.*" + ENV_NAME1 + ".*" + ENV_NAME2 + ".*"));
  }

  @Test
  public void testJoinSqlQuotedStrings()
  {
    assertEquals("'env1', 'env2'", environmentDAO.joinSqlQuotedStrings(new String[] { ENV_NAME1, ENV_NAME2 }));
  }

  @Test
  public void testFindAll()
  {
    List<Environment> envs = environmentDAO.findAll();

    verify(mockEntityManager).createQuery((String) argThat(is(allOf(containsString("SELECT"), not(containsString("WHERE"))))));
  }
}