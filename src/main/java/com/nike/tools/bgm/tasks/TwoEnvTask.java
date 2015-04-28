package com.nike.tools.bgm.tasks;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;

/**
 * A task that reads the datamodel for both live env and stage env, and might write stage env.  Live env should
 * already be fully populated (env, vm, app, db); the stage env should be as well except for the Application record.
 */
public abstract class TwoEnvTask extends TaskImpl
{
  @Autowired
  protected EnvironmentTx environmentTx;

  private String liveEnvName;
  private String stageEnvName;

  protected Environment liveEnv;
  protected ApplicationVm liveApplicationVm;
  protected Application liveApplication;
  protected PhysicalDatabase livePhysicalDatabase;
  protected Environment stageEnv;
  protected ApplicationVm stageApplicationVm;
  protected PhysicalDatabase stagePhysicalDatabase;

  protected Task assign(int position, String liveEnvName, String stageEnvName)
  {
    if (StringUtils.equals(liveEnvName, stageEnvName))
    {
      throw new IllegalArgumentException("Live env must be different from stage env, cannot target env '" + liveEnvName + "' for both");
    }
    super.assign(position);
    this.liveEnvName = liveEnvName;
    this.stageEnvName = stageEnvName;
    return this;
  }

  /**
   * Loads datamodel entities and asserts preconditions on them.  These assertions should be true at the moment when
   * this task is about to begin processing.
   * <p/>
   * Looks up the two environments by name.
   * Currently requires that the envs have exactly one applicationVm, one logical/physicaldb, and that the live env
   * has exactly one application.
   */
  protected void loadDataModel()
  {
    this.liveEnv = environmentTx.findNamedEnv(liveEnvName);
    this.stageEnv = environmentTx.findNamedEnv(stageEnvName);
    this.liveApplicationVm = findApplicationVmFromEnvironment(liveEnv);
    this.liveApplication = findApplicationFromVm(this.liveApplicationVm);
    this.stageApplicationVm = findApplicationVmFromEnvironment(stageEnv);
    this.livePhysicalDatabase = findPhysicalDatabaseFromEnvironment(liveEnv);
    this.stagePhysicalDatabase = findPhysicalDatabaseFromEnvironment(stageEnv);
  }

  protected String context(Environment environment)
  {
    return "[Environment '" + environment.getEnvName() + "']: ";
  }

  /**
   * Gets the env's persisted application vm record.  (Currently support only 1.)
   */
  private ApplicationVm findApplicationVmFromEnvironment(Environment environment)
  {
    List<ApplicationVm> applicationVms = environment.getApplicationVms();
    if (CollectionUtils.isEmpty(applicationVms))
    {
      throw new IllegalStateException(context(environment) + "No application vms");
    }
    else
    {
      if (applicationVms.size() > 1)
      {
        throw new UnsupportedOperationException(context(environment) + "Currently only support case of 1 applicationVm, but environment '"
            + environment.getEnvName() + "' has " + applicationVms.size());
      }
      return applicationVms.get(0);
    }
  }

  /**
   * Gets the applicationVm's persisted application record.  (Currently require exactly 1.)
   */
  private Application findApplicationFromVm(ApplicationVm applicationVm)
  {
    List<Application> applications = applicationVm.getApplications();
    if (CollectionUtils.isEmpty(applications))
    {
      throw new IllegalStateException(context(applicationVm.getEnvironment()) + "No application");
    }
    else
    {
      if (applications.size() > 1)
      {
        throw new UnsupportedOperationException(context(applicationVm.getEnvironment())
            + "Currently only support case of 1 application, but applicationVm '"
            + applicationVm.getHostname() + "' has " + applications.size());
      }
      return applications.get(0);
    }
  }

  /**
   * Gets the env's persisted physicaldb record.  Requires exactly 1.
   */
  private PhysicalDatabase findPhysicalDatabaseFromEnvironment(Environment environment)
  {
    LogicalDatabase logicalDatabase = findLogicalDatabaseFromEnvironment(environment);
    return logicalDatabase.getPhysicalDatabase();
  }

  /**
   * Gets the env's persisted logicaldb record.  Requires exactly 1.
   */
  private LogicalDatabase findLogicalDatabaseFromEnvironment(Environment environment)
  {
    List<LogicalDatabase> logicalDatabases = environment.getLogicalDatabases();
    if (CollectionUtils.isEmpty(logicalDatabases))
    {
      throw new IllegalStateException(context(environment) + "No logical databases");
    }
    else if (logicalDatabases.size() > 1)
    {
      //TODO - start sharing code with RDSSnapshotRestoreTask et al
      throw new UnsupportedOperationException(context(environment) + "Currently only support case of 1 logicalDatabase, but live env has "
          + logicalDatabases.size()); // + ": " + listOfNames(logicalDatabases));
    }
    else if (StringUtils.isBlank(logicalDatabases.get(0).getLogicalName()))
    {
      throw new IllegalStateException(context(environment) + "Logical database has blank name");
    }
    return logicalDatabases.get(0);
  }

}
