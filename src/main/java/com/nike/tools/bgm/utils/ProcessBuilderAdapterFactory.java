package com.nike.tools.bgm.utils;

import org.springframework.stereotype.Component;

/**
 * Simple factory class that constructs a ProcessBuilderAdapter.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class ProcessBuilderAdapterFactory
{
  public ProcessBuilderAdapter create(String[] commandTokens)
  {
    return new ProcessBuilderAdapter(commandTokens);
  }
}
