package bluegreen.manager.utils;

import org.springframework.stereotype.Component;

/**
 * Invokes Thread.sleep.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class ThreadSleeper
{
  public void sleep(long milliseconds) throws InterruptedException
  {
    Thread.sleep(milliseconds);
  }
}
