package bluegreen.manager.substituter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.TwoEnvLoader;
import static bluegreen.manager.substituter.SubstitutionKeys.APPLICATION_VM_MAP;
import static bluegreen.manager.substituter.SubstitutionKeys.LIVE_ENV;
import static bluegreen.manager.substituter.SubstitutionKeys.PHYSICAL_DB_MAP;
import static bluegreen.manager.substituter.SubstitutionKeys.STAGE_ENV;

/**
 * Adds to the base string substituter impl by defining variables related to datamodel entity values involving two
 * environments (live and stage envs).
 */
@Lazy
@Component
@Scope("prototype")
public class TwoEnvStringSubstituter extends StringSubstituterBaseImpl
{
  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private String liveEnvName;
  private String stageEnvName;
  private Map<String, String> extraSubstitutions;
  private TwoEnvLoader twoEnvLoader;

  public TwoEnvStringSubstituter(String liveEnvName, String stageEnvName, Map<String, String> extraSubstitutions)
  {
    this.liveEnvName = liveEnvName;
    this.stageEnvName = stageEnvName;
    this.extraSubstitutions = extraSubstitutions;
  }

  @Override
  public void loadDataModel()
  {
    this.twoEnvLoader = envLoaderFactory.createTwo(liveEnvName, stageEnvName);
    twoEnvLoader.loadDataModel();
    prepareSubstitutions();
  }

  private void prepareSubstitutions()
  {
    substitutions = new HashMap<String, String>();
    if (extraSubstitutions != null)
    {
      substitutions.putAll(extraSubstitutions);
    }
    substitutions.put(LIVE_ENV, liveEnvName);
    substitutions.put(STAGE_ENV, stageEnvName);
    substitutions.put(APPLICATION_VM_MAP, makeApplicationVmMapString());
    substitutions.put(PHYSICAL_DB_MAP, makePhysicalDbMapString());
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
