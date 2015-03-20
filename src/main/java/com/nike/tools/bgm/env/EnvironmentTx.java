package com.nike.tools.bgm.env;

import java.util.List;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.dao.EnvironmentDAO;
import com.nike.tools.bgm.model.domain.Environment;

/**
 * Serves transactional db queries related to the Environment hierarchy, includes Logical/PhysicalDatabase
 * and ApplicationVm.
 */
@Transactional
@Component
public class EnvironmentTx
{
  @Autowired
  private EnvironmentDAO environmentDAO;

  /**
   * Looks up all the specified names in the environment table, returning true for the names that exist and
   * false otherwise.  Return array in same order as input array.
   */
  public boolean[] checkIfEnvNamesExist(String... envNames)
  {
    if (envNames == null)
    {
      return null;
    }
    boolean[] exists = new boolean[envNames.length];
    List<Environment> environments = environmentDAO.findNamedEnvs(envNames);
    for (int idx = 0; idx < envNames.length; ++idx)
    {
      if (listHasNamedEnv(environments, envNames[idx]))
      {
        exists[idx] = true;
      }
    }
    return exists;
  }

  /**
   * Returns true if the input list has the named environment.
   */
  private boolean listHasNamedEnv(List<Environment> environments, String envName)
  {
    if (environments != null)
    {
      for (Environment environment : environments)
      {
        if (StringUtils.equals(environment.getEnvName(), envName))
        {
          return true;
        }
      }
    }
    return false;
  }

}
