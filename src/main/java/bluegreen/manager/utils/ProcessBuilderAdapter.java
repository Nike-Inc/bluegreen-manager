package bluegreen.manager.utils;

import java.io.IOException;

/**
 * Adapter pattern: wraps the final class ProcessBuilder as a non-final so that it can be mocked by mockito unit test.
 * (Unit test intruding again onto core code, sigh)
 */
public class ProcessBuilderAdapter
{
  private ProcessBuilder processBuilder;

  /**
   * Adapter to {@link java.lang.ProcessBuilder#ProcessBuilder(java.lang.String...)}
   */
  public ProcessBuilderAdapter(String... command)
  {
    processBuilder = new ProcessBuilder(command);
  }

  /**
   * Adapter to {@link java.lang.ProcessBuilder#start()}
   */
  public Process start() throws IOException
  {
    return processBuilder.start();
  }

  /**
   * Adapter to {@link java.lang.ProcessBuilder#redirectErrorStream(boolean)}
   */
  public ProcessBuilderAdapter redirectErrorStream(boolean value)
  {
    processBuilder.redirectErrorStream(value);
    return this; //I verified that JDK1.7 redirectErrorStream also does 'return this'
  }
}
