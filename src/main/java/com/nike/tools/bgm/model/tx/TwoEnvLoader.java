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
 * Loads the datamodel entities for two envs (live and stage) and asserts preconditions on the entities.
 * Live env should already be fully populated (env, vm, app, db); the stage env should be as well except for the
 * Application record.
 * <p/>
 * Currently requires that the envs have exactly one applicationVm, one logical/physicaldb, and that the live env
 * has exactly one application.
 */
@Lazy
@Component
@Scope("prototype")
public class TwoEnvLoader
{
  @Autowired
  private EnvironmentTx environmentTx;

  private String liveEnvName;
  private String stageEnvName;

  private Environment liveEnv;
  private ApplicationVm liveApplicationVm;
  private Application liveApplication;
  private PhysicalDatabase livePhysicalDatabase;
  private Environment stageEnv;
  private ApplicationVm stageApplicationVm;
  private PhysicalDatabase stagePhysicalDatabase;

  /**
   * Loads datamodel entities and asserts preconditions on them.
   */
  public void loadDataModel()
  {
    this.liveEnv = environmentTx.findNamedEnv(liveEnvName);
    this.stageEnv = environmentTx.findNamedEnv(stageEnvName);
    this.liveApplicationVm = findApplicationVmFromEnvironment(liveEnv);
    this.liveApplication = findApplicationFromVm(this.liveApplicationVm);
    this.stageApplicationVm = findApplicationVmFromEnvironment(stageEnv);
    this.livePhysicalDatabase = findPhysicalDatabaseFromEnvironment(liveEnv);
    this.stagePhysicalDatabase = findPhysicalDatabaseFromEnvironment(stageEnv);
  }

  private String context(Environment environment)
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

  public String getLiveEnvName()
  {
    return liveEnvName;
  }

  public void setLiveEnvName(String liveEnvName)
  {
    this.liveEnvName = liveEnvName;
  }

  public String getStageEnvName()
  {
    return stageEnvName;
  }

  public void setStageEnvName(String stageEnvName)
  {
    this.stageEnvName = stageEnvName;
  }

  public Environment getLiveEnv()
  {
    return liveEnv;
  }

  public ApplicationVm getLiveApplicationVm()
  {
    return liveApplicationVm;
  }

  public Application getLiveApplication()
  {
    return liveApplication;
  }

  public PhysicalDatabase getLivePhysicalDatabase()
  {
    return livePhysicalDatabase;
  }

  public Environment getStageEnv()
  {
    return stageEnv;
  }

  public ApplicationVm getStageApplicationVm()
  {
    return stageApplicationVm;
  }

  public PhysicalDatabase getStagePhysicalDatabase()
  {
    return stagePhysicalDatabase;
  }
}
