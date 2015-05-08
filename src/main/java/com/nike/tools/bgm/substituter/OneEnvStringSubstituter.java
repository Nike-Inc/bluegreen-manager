package com.nike.tools.bgm.substituter;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.tx.EnvLoaderFactory;
import com.nike.tools.bgm.model.tx.OneEnvLoader;

/**
 * Makes string substitutions of %{..} variables using datamodel entity values involving one environment.
 * <p/>
 * Variables supported: vmHostname.
 * Also any extraSubstitutions.
 */
@Lazy
@Component
@Scope("prototype")
public class OneEnvStringSubstituter extends StringSubstituterExtraImpl
{
  /**
   * Variable to be substituted with the name of the target environment.
   */
  private static final String CMDVAR_ENV = "%{env}";

  /**
   * Variable to be substituted with the application vm hostname in this env.
   * (Assumes there is exactly 1 applicationVm.)
   */
  private static final String CMDVAR_VM_HOSTNAME = "%{vmHostname}";

  @Autowired
  private EnvLoaderFactory envLoaderFactory;

  private String envName;
  private OneEnvLoader oneEnvLoader;

  public OneEnvStringSubstituter()
  {
  }

  public OneEnvStringSubstituter(String envName, Map<String, String> extraSubstitutions)
  {
    super(extraSubstitutions);
    this.envName = envName;
  }

  /**
   * Currently asserting the "1 application exists" precondition.
   */
  @Override
  public void loadDataModel()
  {
    oneEnvLoader = envLoaderFactory.createOne(envName);
    oneEnvLoader.loadApplication();
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
    substituted = StringUtils.replace(substituted, CMDVAR_ENV, envName);
    substituted = StringUtils.replace(substituted, CMDVAR_VM_HOSTNAME, oneEnvLoader.getApplicationVm().getHostname());
    return substituteExtra(substituted);
  }
}
