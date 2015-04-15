package com.nike.tools.bgm.tasks;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;

/**
 * A task that communicates with an applicationVm, in an existing environment.
 */
public abstract class ApplicationVmTask extends TaskImpl
{
  @Autowired
  private EnvironmentTx environmentTx;

  protected Environment environment;
  protected ApplicationVm applicationVm;

  /**
   * Looks up the environment entity by name.
   *
   * @param createVm True if this task is intended to create the applicationVm, false if it is expected to already exist.
   */
  protected Task init(int position, String envName, boolean createVm)
  {
    super.init(position);
    this.environment = environmentTx.findNamedEnv(envName);

    findApplicationVmFromEnvironment(createVm);

    return this;
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
      sb.append(applicationVm.getHostname());
    }
    sb.append("]: ");
    return sb.toString();
  }

  /**
   * Gets the env's persisted application vm record.  (Currently support only 1.)
   */
  protected void findApplicationVmFromEnvironment(boolean createVm)
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

}
