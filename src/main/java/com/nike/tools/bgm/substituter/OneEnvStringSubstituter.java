package com.nike.tools.bgm.substituter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

import static com.nike.tools.bgm.substituter.SubstitutionKeys.ENV;
import static com.nike.tools.bgm.substituter.SubstitutionKeys.VM_HOSTNAME;

/**
 * Adds to the base string substituter impl by defining variables related to datamodel entity values involving one
 * environment.
 */
@Lazy
@Component
@Scope("prototype")
public class OneEnvStringSubstituter extends StringSubstituterBaseImpl
{
  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private String envName;
  private Map<String, String> extraSubstitutions;
  private OneEnvLoader oneEnvLoader;

  public OneEnvStringSubstituter(String envName, Map<String, String> extraSubstitutions)
  {
    this.envName = envName;
    this.extraSubstitutions = extraSubstitutions;
  }

  /**
   * Currently asserting the "1 application exists" precondition.
   */
  @Override
  public void loadDataModel()
  {
    oneEnvLoader = envLoaderFactory.createOne(envName);
    oneEnvLoader.loadApplication();
    prepareSubstitutions();
  }

  private void prepareSubstitutions()
  {
    substitutions = new HashMap<String, String>();
    if (extraSubstitutions != null)
    {
      substitutions.putAll(extraSubstitutions);
    }
    substitutions.put(ENV, envName);
    substitutions.put(VM_HOSTNAME, oneEnvLoader.getApplicationVm().getHostname());
  }

}
