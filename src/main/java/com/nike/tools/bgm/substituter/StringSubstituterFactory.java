package com.nike.tools.bgm.substituter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory that can create a string substituter.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class StringSubstituterFactory
{
  @Autowired
  protected ApplicationContext applicationContext;

  public ZeroEnvStringSubstituter createZero(Map<String, String> extraSubstitutions)
  {
    return applicationContext.getBean(ZeroEnvStringSubstituter.class, extraSubstitutions);
  }

  public OneEnvStringSubstituter createOne(String envName, Map<String, String> extraSubstitutions)
  {
    return applicationContext.getBean(OneEnvStringSubstituter.class, envName, extraSubstitutions);
  }

  public TwoEnvStringSubstituter createTwo(String liveEnvName,
                                           String stageEnvName,
                                           Map<String, String> extraSubstitutions)
  {
    return applicationContext.getBean(TwoEnvStringSubstituter.class, liveEnvName, stageEnvName, extraSubstitutions);
  }

}
