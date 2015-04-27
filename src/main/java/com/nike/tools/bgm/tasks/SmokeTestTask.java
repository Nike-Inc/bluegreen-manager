package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nike.tools.bgm.model.domain.TaskStatus;

/**
 * Touches the application's bluegreen client api just to make sure the endpoint is alive.
 */
public class SmokeTestTask extends ApplicationTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTestTask.class);

  public Task init(int position, String envName)
  {
    super.init(position, envName);
    return this;
  }

  /**
   * Touches the application's bluegreen client api just to make sure the endpoint is alive.
   * <p/>
   * "Touch" = authenticate to the application.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    LOGGER.info(context() + "Touching application to see that it is alive" + noopRemark(noop));
    if (!noop)
    {
      initApplicationSession();
    }
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }
}
