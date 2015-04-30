package com.nike.tools.bgm.tasks;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.client.app.ApplicationClient;
import com.nike.tools.bgm.client.app.ApplicationSession;
import com.nike.tools.bgm.model.domain.Application;

/**
 * A task that communicates with an existing application, in an applicationVm, in an environment.
 */
public abstract class ApplicationTask extends ApplicationVmTask
{
  @Autowired
  protected ApplicationClient applicationClient;

  protected ApplicationSession applicationSession;

  protected Application application;

  public Task assign(int position, String envName)
  {
    super.assign(position, envName, false/*createVm*/);
    return this;
  }

  /**
   * Loads datamodel entities and asserts preconditions on them.  These assertions should be true at the moment when
   * this task is about to begin processing.
   * <p/>
   * Looks up the environment entity by name.
   * Currently requires that the env has exactly one applicationVm and one application.
   */
  protected void loadDataModel()
  {
    super.loadDataModel();
    findApplicationFromVm();
  }

  /**
   * Returns a string that describes the known environment context, for logging purposes.
   */
  @Override
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
   * Initializes an authenticated session with the application.
   * <p/>
   * Could be called later than {@link #assign(int, String)}.
   */
  void initApplicationSession()
  {
    applicationSession = applicationClient.authenticate(application);
  }

}
