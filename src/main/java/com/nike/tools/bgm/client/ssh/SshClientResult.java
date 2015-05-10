package com.nike.tools.bgm.client.ssh;

import org.apache.commons.lang3.StringUtils;

/**
 * Final output and return code of the ssh client execution.
 */
public class SshClientResult
{
  private String output;
  private int exitValue;

  public SshClientResult(String output, int exitValue)
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
    if (obj instanceof SshClientResult)
    {
      SshClientResult other = (SshClientResult) obj;
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
}
