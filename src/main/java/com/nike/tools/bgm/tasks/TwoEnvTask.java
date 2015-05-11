package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;
import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.TwoEnvLoader;

/**
 * A task that reads the datamodel for both live env and stage env, and might write stage env.  Live env should
 * already be fully populated (env, vm, app, db); the stage env should be as well except for the Application record.
 */
public abstract class TwoEnvTask extends TaskImpl
{
  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private String liveEnvName;
  private String stageEnvName;
  private TwoEnvLoader twoEnvLoader;

  protected Environment liveEnv;
  protected ApplicationVm liveApplicationVm;
  protected Application liveApplication;
  protected PhysicalDatabase livePhysicalDatabase;
  protected Environment stageEnv;
  protected ApplicationVm stageApplicationVm;
  protected PhysicalDatabase stagePhysicalDatabase;

  public Task assign(int position, String liveEnvName, String stageEnvName)
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
    twoEnvLoader = envLoaderFactory.createTwo(liveEnvName, stageEnvName);
    twoEnvLoader.loadDataModel();

    this.liveEnv = twoEnvLoader.getLiveEnv();
    this.liveApplicationVm = twoEnvLoader.getLiveApplicationVm();
    this.liveApplication = twoEnvLoader.getLiveApplication();
    this.livePhysicalDatabase = twoEnvLoader.getLivePhysicalDatabase();
    this.stageEnv = twoEnvLoader.getStageEnv();
    this.stageApplicationVm = twoEnvLoader.getStageApplicationVm();
    this.stagePhysicalDatabase = twoEnvLoader.getStagePhysicalDatabase();
  }

  protected String context(Environment environment)
  {
    return "[Environment '" + environment.getEnvName() + "']: ";
  }

  protected String context()
  {
    return "[Live Env '" + liveEnvName + "', Stage Env '" + stageEnvName + "']: ";
  }
}
