package com.nike.tools.bgm.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Final output and return code of a shell command invocation.
 */
public class ShellResult
{
  private static final String HYPHEN_LINE = "----------------------------------------------------------------------";

  private String output;
  private int exitValue;

  public ShellResult(String output, int exitValue)
  {
    this.output = output;
    this.exitValue = exitValue;
  }

  public String getOutput()
  {
    return output;
  }

  public int getExitValue()
  {
    return exitValue;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof ShellResult)
    {
      ShellResult other = (ShellResult) obj;
      return StringUtils.equals(output, other.output) && exitValue == other.exitValue;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int hash = 17;
    hash = hash * prime + (output == null ? 0 : output.hashCode());
    hash = (hash + exitValue) * prime;
    return hash;
  }

  /**
   * Formats the shell output and exitvalue in a multi-line string.
   */
  public String describe()
  {
    return HYPHEN_LINE + "\n" + closeWithNewline(output) + HYPHEN_LINE + "\nExit Value: " + exitValue;
  }

  /**
   * Returns the input string plus a newline if it does not already end with one.
   */
  private String closeWithNewline(String str)
  {
    if (str == null)
    {
      return str;
    }
    if (!str.endsWith("\n"))
    {
      return str + "\n";
      //Incomplete, doesn't solve for case where ssh host is windows ...but this is just for debug assistance anyway
    }
    return str;
  }

}
