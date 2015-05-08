package com.nike.tools.bgm.model.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory that can create an env loader object.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class EnvLoaderFactory
{
  @Autowired
  protected ApplicationContext applicationContext;

  public OneEnvLoader createOne(String envName)
  {
    OneEnvLoader oneEnvLoader = applicationContext.getBean(OneEnvLoader.class);
    oneEnvLoader.setEnvName(envName);
    return oneEnvLoader;
  }

  public TwoEnvLoader createTwo(String liveEnvName, String stageEnvName)
  {
    TwoEnvLoader twoEnvLoader = applicationContext.getBean(TwoEnvLoader.class);
    twoEnvLoader.setLiveEnvName(liveEnvName);
    twoEnvLoader.setStageEnvName(stageEnvName);
    return twoEnvLoader;
  }
}
