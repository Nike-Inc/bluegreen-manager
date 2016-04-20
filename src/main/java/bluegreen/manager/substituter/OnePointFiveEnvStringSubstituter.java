package bluegreen.manager.substituter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.OneEnvLoader;
import static bluegreen.manager.substituter.SubstitutionKeys.LIVE_ENV;
import static bluegreen.manager.substituter.SubstitutionKeys.PHYSICAL_DB_MAP;
import static bluegreen.manager.substituter.SubstitutionKeys.STAGE_ENV;

/**
 * Adds to the base string substituter impl by defining variables related to datamodel entity values involving one
 * full environment and a second environment with a Database created but no VMs created.
 */
@Lazy
@Component
@Scope("prototype")
public class OnePointFiveEnvStringSubstituter extends StringSubstituterBaseImpl {
  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private String liveEnvName;
  private String stageEnvName;
  private Map<String, String> extraSubstitutions;
  private OneEnvLoader liveEnvLoader;
  private OneEnvLoader stageEnvLoader;

  public OnePointFiveEnvStringSubstituter(String liveEnvName,
                                          String stageEnvName,
                                          Map<String, String> extraSubstitutions) {
    this.liveEnvName = liveEnvName;
    this.stageEnvName = stageEnvName;
    this.extraSubstitutions = extraSubstitutions;
  }

  /**
   * Currently asserting the "2 databases exist" precondition.
   */
  @Override
  public void loadDataModel() {
    liveEnvLoader = envLoaderFactory.createOne(liveEnvName);
    stageEnvLoader = envLoaderFactory.createOne(stageEnvName);

    liveEnvLoader.loadPhysicalDatabase();
    stageEnvLoader.loadPhysicalDatabase();
    prepareSubstitutions();
  }

  private void prepareSubstitutions() {
    substitutions = new HashMap<String, String>();
    if (extraSubstitutions != null) {
      substitutions.putAll(extraSubstitutions);
    }
    substitutions.put(LIVE_ENV, liveEnvName);
    substitutions.put(STAGE_ENV, stageEnvName);
    substitutions.put(PHYSICAL_DB_MAP, makePhysicalDbMapString());
  }

  /**
   * Makes a comma-delimited list of two physicaldb properties:
   * livePhysicalInstName,stagePhysicalInstName
   * <p/>
   * Currently only supports mapping 1 physicaldb from live to stage.
   */
  private String makePhysicalDbMapString() {
    StringBuilder sb = new StringBuilder();
    sb.append(liveEnvLoader.getPhysicalDatabase().getInstanceName());
    sb.append(",");
    sb.append(stageEnvLoader.getPhysicalDatabase().getInstanceName());
    return sb.toString();
  }
}