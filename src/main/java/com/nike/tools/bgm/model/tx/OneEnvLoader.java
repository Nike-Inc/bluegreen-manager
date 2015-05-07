package com.nike.tools.bgm.model.tx;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;

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

  private String envName;
  private Environment environment;
  private ApplicationVm applicationVm;
  private Application application;

  /**
   * Loads the environment on the assumption that there is exactly 1 application vm, or that there are 0 vms.
   * No attempt is made to load an application or database.
   */
  public void loadApplicationVm(boolean createVm)
  {
    this.environment = environmentTx.findNamedEnv(envName);
    findApplicationVmFromEnvironment(createVm);
  }

  /**
   * Loads the environment on the assumption that there is exactly 1 application vm and 1 application.
   * No attempt is made to load a database.
   */
  public void loadApplication()
  {
    loadApplicationVm(false/*i.e. modify vm*/);
    findApplicationFromVm();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
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
}
