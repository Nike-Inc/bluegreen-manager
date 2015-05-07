package com.nike.tools.bgm.substituter;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Accepts an optional plain old map of "extra substitutions".
 */
public abstract class StringSubstituterExtraImpl implements StringSubstituter
{
  protected Map<String, String> extraSubstitutions;

  public StringSubstituterExtraImpl()
  {
  }

  public StringSubstituterExtraImpl(Map<String, String> extraSubstitutions)
  {
    this.extraSubstitutions = extraSubstitutions; //TODO- prefer deep copy
  }

  /**
   * Performs the optional extra substitutions on the command and returns the result.
   */
  protected String substituteExtra(String command)
  {
    if (extraSubstitutions != null)
    {
      for (Map.Entry<String, String> entry : extraSubstitutions.entrySet())
      {
        command = StringUtils.replace(command, entry.getKey(), entry.getValue());
      }
    }
    return command;
  }

  public Map<String, String> getExtraSubstitutions()
  {
    return extraSubstitutions;
  }

  public void setExtraSubstitutions(Map<String, String> extraSubstitutions)
  {
    this.extraSubstitutions = extraSubstitutions; //TODO- prefer deep copy
  }
}
