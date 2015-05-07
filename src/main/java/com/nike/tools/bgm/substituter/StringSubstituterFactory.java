package com.nike.tools.bgm.substituter;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Factory that can create a string substituter.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class StringSubstituterFactory
{
  public OneEnvStringSubstituter createOne(String envName, Map<String, String> extraSubstitutions)
  {
    return new OneEnvStringSubstituter(envName, extraSubstitutions);
  }

  public TwoEnvStringSubstituter createTwo(String liveEnvName,
                                           String stageEnvName,
                                           Map<String, String> extraSubstitutions)
  {
    return new TwoEnvStringSubstituter(liveEnvName, stageEnvName, extraSubstitutions);
  }

}
