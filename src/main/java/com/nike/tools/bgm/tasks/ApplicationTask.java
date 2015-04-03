package com.nike.tools.bgm.tasks;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;

/**
 * A task that communicates with an application, in an applicationVm, in an environment.
 */
public abstract class ApplicationTask extends TaskImpl
{
  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  protected ApplicationClient applicationClient;

  protected Environment environment;
  protected ApplicationVm applicationVm;
  protected Application application;

  /**
   * Looks up the environment entity by name.
   * Currently requires that the env has exactly one applicationVm and one application.
   */
  protected void init(int position, String envName)
  {
    super.init(position);
    this.environment = environmentTx.findNamedEnv(envName);

    findApplicationVmFromEnvironment();
    findApplicationFromVm();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   */
  String context()
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
  private void findApplicationVmFromEnvironment()
  {
    List<ApplicationVm> applicationVms = environment.getApplicationVms();
    if (CollectionUtils.isEmpty(applicationVms))
    {
      throw new IllegalStateException(context() + "No application vms");
    }
    else if (applicationVms.size() > 1)
    {
      throw new UnsupportedOperationException(context() + "Currently only support case of 1 applicationVm, but found "
          + applicationVms.size());
    }
    this.applicationVm = applicationVms.get(0);
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

}
