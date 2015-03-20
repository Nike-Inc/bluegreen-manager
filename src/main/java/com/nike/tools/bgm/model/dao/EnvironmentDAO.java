package com.nike.tools.bgm.model.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.nike.tools.bgm.model.domain.Environment;

@Repository
public class EnvironmentDAO extends GenericDAO<Environment>
{
  /**
   * Returns a list of the named environments (assuming they exist).
   */
  public List<Environment> findNamedEnvs(String... envNames)
  {
    if (envNames == null)
    {
      return null;
    }
    else if (envNames.length == 0)
    {
      return new ArrayList<Environment>();
    }
    String queryString = "SELECT e FROM " + Environment.class.getSimpleName() + " e WHERE "
        + "e.envName IN (" + joinSqlQuotedStrings(envNames) + ")";
    return entityManager.createQuery(queryString).getResultList();
  }

  /**
   * Converts the input list of strings to one comma-delimited string with individual tokens quoted with sql apostrophes.
   */
  private String joinSqlQuotedStrings(String[] strings)
  {
    if (strings == null)
    {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (String string : strings)
    {
      if (sb.length() > 0)
      {
        sb.append(", ");
      }
      sb.append("'" + string + "'");
    }
    return sb.toString();
  }

  /**
   * Returns all environments.
   */
  public List<Environment> findAll()
  {
    return entityManager.createQuery("SELECT OBJECT(e) FROM " + Environment.class.getSimpleName() + " as e").getResultList();
  }

}
