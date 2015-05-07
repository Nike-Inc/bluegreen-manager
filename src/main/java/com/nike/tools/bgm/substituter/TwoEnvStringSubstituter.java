package com.nike.tools.bgm.substituter;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.TwoEnvLoader;

/**
 * Makes string substitutions of %{..} variables using datamodel entity values involving two environments
 * (live and stage envs).
 * <p/>
 * Variables supported: liveEnv, stageEnv, applicationVmMap, physicalDbMap.
 * Also any extraSubstitutions.
 */
public class TwoEnvStringSubstituter extends StringSubstituterExtraImpl
{
  /**
   * Variable to be substituted with the name of the live environment.
   */
  private static final String CMDVAR_LIVE_ENV = "%{liveEnv}";

  /**
   * Variable to be substituted with the name of the stage environment.
   */
  private static final String CMDVAR_STAGE_ENV = "%{stageEnv}";

  /**
   * Variable to be substituted with a comma-delimited list of four applicationVm properties:
   * liveHostname,liveIpAddress,stageHostname,stageIpAddress.
   * <p/>
   * Currently only supports mapping 1 vm from live to stage.
   */
  private static final String CMDVAR_APPLICATION_VM_MAP = "%{applicationVmMap}";

  /**
   * Variable to be substituted with a comma-delimited list of two physicaldb properties:
   * livePhysicalInstName,stagePhysicalInstName
   * <p/>
   * Currently only supports mapping 1 physicaldb from live to stage.
   */
  private static final String CMDVAR_PHYSICAL_DB_MAP = "%{physicalDbMap}";

  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private String liveEnvName;
  private String stageEnvName;
  private Map<String, String> extraSubstitutions;
  private TwoEnvLoader twoEnvLoader;

  public TwoEnvStringSubstituter()
  {
    super();
  }

  public TwoEnvStringSubstituter(String liveEnvName, String stageEnvName, Map<String, String> extraSubstitutions)
  {
    super(extraSubstitutions);
    this.liveEnvName = liveEnvName;
    this.stageEnvName = stageEnvName;
  }

  @Override
  public void loadDataModel()
  {
    this.twoEnvLoader = envLoaderFactory.createTwo(liveEnvName, stageEnvName);
    twoEnvLoader.loadDataModel();
  }

  /**
   * Substitutes variables of the form '%{vblname}' in the original string, returns the replaced version.
   */
  @Override
  public String substituteVariables(String command)
  {
    if (StringUtils.isBlank(command))
    {
      throw new IllegalArgumentException("Command is blank");
    }
    String substituted = command;
    substituted = StringUtils.replace(substituted, CMDVAR_LIVE_ENV, liveEnvName);
    substituted = StringUtils.replace(substituted, CMDVAR_STAGE_ENV, stageEnvName);
    substituted = StringUtils.replace(substituted, CMDVAR_APPLICATION_VM_MAP, makeApplicationVmMapString());
    substituted = StringUtils.replace(substituted, CMDVAR_PHYSICAL_DB_MAP, makePhysicalDbMapString());
    return substituteExtra(substituted);
  }

  /**
   * Makes a comma-delimited list of four applicationVm properties:
   * liveHostname,liveIpAddress,stageHostname,stageIpAddress.
   * <p/>
   * Currently only supports mapping 1 vm from live to stage.
   */
  private String makeApplicationVmMapString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(twoEnvLoader.getLiveApplicationVm().getHostname());
    sb.append(",");
    sb.append(twoEnvLoader.getLiveApplicationVm().getIpAddress());
    sb.append(",");
    sb.append(twoEnvLoader.getStageApplicationVm().getHostname());
    sb.append(",");
    sb.append(twoEnvLoader.getStageApplicationVm().getIpAddress());
    return sb.toString();
  }

  /**
   * Makes a comma-delimited list of two physicaldb properties:
   * livePhysicalInstName,stagePhysicalInstName
   * <p/>
   * Currently only supports mapping 1 physicaldb from live to stage.
   */
  private String makePhysicalDbMapString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(twoEnvLoader.getLivePhysicalDatabase().getInstanceName());
    sb.append(",");
    sb.append(twoEnvLoader.getStagePhysicalDatabase().getInstanceName());
    return sb.toString();
  }

}
