package com.nike.tools.bgm.model.tx;

import org.springframework.stereotype.Component;

/**
 * Factory that can create an env loader object.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class EnvLoaderFactory
{
  public OneEnvLoader createOne(String envName)
  {
    OneEnvLoader oneEnvLoader = new OneEnvLoader();
    oneEnvLoader.setEnvName(envName);
    return oneEnvLoader;
  }

  public TwoEnvLoader createTwo(String liveEnvName, String stageEnvName)
  {
    TwoEnvLoader twoEnvLoader = new TwoEnvLoader();
    twoEnvLoader.setLiveEnvName(liveEnvName);
    twoEnvLoader.setStageEnvName(stageEnvName);
    return twoEnvLoader;
  }
}
