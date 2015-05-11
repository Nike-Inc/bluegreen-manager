package com.nike.tools.bgm.model.tx;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;

/**
 * Loads datamodel entities for one environment and asserts preconditions.  How much to load is decided by the caller.
 */
@Lazy
@Component
@Scope("prototype")
public class OneEnvLoader
{
  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private EnvironmentHelper environmentHelper;

  private String envName;
  private Environment environment;
  private ApplicationVm applicationVm;
  private Application application;
  private LogicalDatabase logicalDatabase;
  private PhysicalDatabase physicalDatabase;

  /**
   * Loads the environment on the assumption that there is exactly 1 application vm, or that there are 0 vms.
   * Does not assert the existence of an application or database.
   */
  public void loadApplicationVm(boolean createVm)
  {
    this.environment = environmentTx.findNamedEnv(envName);
    findApplicationVmFromEnvironment(createVm);
  }

  /**
   * Loads the environment on the assumption that there is exactly 1 application vm and 1 application.
   * Does not assert the existence of a database.
   */
  public void loadApplication()
  {
    loadApplicationVm(false/*i.e. modify vm*/);
    findApplicationFromVm();
  }

  /**
   * Loads the environment on the assumption that there is exactly 1 logicaldb and 1 physicaldb.
   * Does not assert the existence of an application vm or application.
   */
  public void loadPhysicalDatabase()
  {
    this.environment = environmentTx.findNamedEnv(envName);
    this.logicalDatabase = findLogicalDatabaseFromEnvironment();
    this.physicalDatabase = logicalDatabase.getPhysicalDatabase();
    checkPhysicalDatabase();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   * <p/>
   * TODO - show the db context, or make separate method for that
   */
  public String context()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[environment '" + environment.getEnvName() + "'");
    if (applicationVm != null)
    {
      sb.append(", ");
      if (application == null)
      {
        sb.append(applicationVm.getHostname());
      }
      else
      {
        sb.append(application.makeHostnameUri());
      }
    }
    sb.append("]: ");
    return sb.toString();
  }

  /**
   * Gets the env's persisted application vm record.  (Currently support only 1.)
   */
  private void findApplicationVmFromEnvironment(boolean createVm)
  {
    List<ApplicationVm> applicationVms = environment.getApplicationVms();
    if (CollectionUtils.isEmpty(applicationVms))
    {
      if (!createVm)
      {
        throw new IllegalStateException(context() + "No application vms");
      }
    }
    else if (createVm)
    {
      throw new IllegalStateException("Cannot create applicationVm, since " + applicationVms.size() + " already exist");
    }
    else
    {
      if (applicationVms.size() > 1)
      {
        throw new UnsupportedOperationException(context() + "Currently only support case of 1 applicationVm, but found "
            + applicationVms.size());
      }
      this.applicationVm = applicationVms.get(0);
    }
  }

  /**
   * Gets the env's persisted application record.  (Currently support only 1.)
   */
  private void findApplicationFromVm()
  {
    List<Application> applications = applicationVm.getApplications();
    if (CollectionUtils.isEmpty(applications))
    {
      throw new IllegalStateException(context() + "No applications");
    }
    else if (applications.size() > 1)
    {
      throw new UnsupportedOperationException(context() + "Currently only support case of 1 application, but found "
          + applications.size());
    }
    this.application = applications.get(0);
  }

  /**
   * Gets the env's persisted logicaldb record.  Requires exactly 1.
   */
  private LogicalDatabase findLogicalDatabaseFromEnvironment()
  {
    List<LogicalDatabase> logicalDatabases = environment.getLogicalDatabases();
    if (CollectionUtils.isEmpty(logicalDatabases))
    {
      throw new IllegalStateException(context() + "No logical databases");
    }
    else if (logicalDatabases.size() > 1)
    {
      throw new UnsupportedOperationException(context() + "Currently only support case of 1 logicalDatabase, but env has "
          + logicalDatabases.size() + ": " + environmentHelper.listOfNames(logicalDatabases));
    }
    else if (StringUtils.isBlank(logicalDatabases.get(0).getLogicalName()))
    {
      throw new IllegalStateException(context() + "Live logical database has blank name");
    }
    return logicalDatabases.get(0);
  }

  /**
   * Sanity checks the physical database.  Callers may need to perform additional task-specific checks.
   */
  private void checkPhysicalDatabase()
  {
    if (physicalDatabase == null)
    {
      throw new IllegalStateException(context() + "No physical database");
    }
    else if (StringUtils.isBlank(physicalDatabase.getInstanceName()))
    {
      throw new IllegalArgumentException(context() + "Physical database has blank instance name");
    }
  }

  public void setEnvName(String envName)
  {
    this.envName = envName;
  }

  public Environment getEnvironment()
  {
    return environment;
  }

  public ApplicationVm getApplicationVm()
  {
    return applicationVm;
  }

  public Application getApplication()
  {
    return application;
  }

  public LogicalDatabase getLogicalDatabase()
  {
    return logicalDatabase;
  }

  public PhysicalDatabase getPhysicalDatabase()
  {
    return physicalDatabase;
  }
}
