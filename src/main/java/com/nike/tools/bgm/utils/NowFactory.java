package com.nike.tools.bgm.utils;

import java.util.Date;

import org.springframework.stereotype.Component;

/**
 * Simple factory class that simply returns the date representing "now".
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class NowFactory
{
  /**
   * Returns the current date/time.
   * <p/>
   * Behavior can be overridden for test purposes.
   */
  public Date now()
  {
    return new Date();
  }
}
