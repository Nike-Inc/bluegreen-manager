package bluegreen.manager.model.tx;

import java.util.List;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.dao.EnvironmentDAO;
import bluegreen.manager.model.domain.Application;
import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.model.domain.Environment;
import bluegreen.manager.model.domain.LogicalDatabase;

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

  /**
   * Finds the named environment, throws if not found.
   * <p/>
   * Since the data cascade is small, actively loads all references: databases, application vms and applications.
   */
  public Environment findNamedEnv(String envName)
  {
    Environment environment = environmentDAO.findNamedEnv(envName);
    activeLoadAll(environment);
    return environment;
  }

  /**
   * Finds the named environment, or null if not found.
   * <p/>
   * Since the data cascade is small, actively loads all references: databases, application vms and applications.
   */
  public Environment findNamedEnvAllowNull(String envName)
  {
    Environment environment = environmentDAO.findNamedEnvAllowNull(envName);
    activeLoadAll(environment);
    return environment;
  }

  /**
   * Actively loads the environment's databases, applications and vms, while the tx is open.
   */
  private void activeLoadAll(Environment environment)
  {
    if (environment != null)
    {
      if (environment.getLogicalDatabases() != null)
      {
        for (LogicalDatabase logicalDatabase : environment.getLogicalDatabases())
        {
          logicalDatabase.getPhysicalDatabase(); //load
        }
      }
      if (environment.getApplicationVms() != null)
      {
        for (ApplicationVm applicationVm : environment.getApplicationVms())
        {
          if (applicationVm.getApplications() != null)
          {
            for (Application application : applicationVm.getApplications())
            {
              //No action other than load
            }
          }
        }
      }
    }
  }

  /**
   * Persists a new environment, including the environment's cascade.
   */
  public void newEnvironment(Environment environment)
  {
    environmentDAO.persist(environment);
  }

  /**
   * Persists changes to the detached environment, including the environment's cascade.
   */
  public void updateEnvironment(Environment environment)
  {
    environmentDAO.merge(environment);
  }

  /**
   * Persists the deletion of the detached environment, including the environment's cascade.
   */
  public void deleteEnvironment(Environment environment)
  {
    /*
    Need to manage the entity before removal.  However for some reason 'environmentDAO.merge(environment)' does not
    manage the entity, entityManager.contains() returns false afterwards!  Have to find() it.
     */
    Environment managedEnvironment = environmentDAO.find(environment.getEnvId());

    environmentDAO.remove(managedEnvironment);
  }
}
