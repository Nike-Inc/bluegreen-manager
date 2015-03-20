package com.nike.tools.bgm.main;

/**
 * Thrown when cmdline args are invalid.
 */
public class CmdlineException extends RuntimeException
{
  public CmdlineException(String s)
  {
    super(s);
  }
}
