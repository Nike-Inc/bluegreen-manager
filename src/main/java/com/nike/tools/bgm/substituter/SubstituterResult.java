package com.nike.tools.bgm.substituter;

/**
 * Holds the substituted and expurgated results of string substitution.
 * <p/>
 * "Substituted" result = all variables fully expanded.
 * "Expurgated" result = same as substituted result but with passwords bleeped out.
 */
public class SubstituterResult
{
  private String substituted;
  private String expurgated;

  public SubstituterResult()
  {
  }

  public SubstituterResult(String substituted, String expurgated)
  {
    this.substituted = substituted;
    this.expurgated = expurgated;
  }

  public String getSubstituted()
  {
    return substituted;
  }

  public void setSubstituted(String substituted)
  {
    this.substituted = substituted;
  }

  public String getExpurgated()
  {
    return expurgated;
  }

  public void setExpurgated(String expurgated)
  {
    this.expurgated = expurgated;
  }

  /**
   * Intentionally returns the unhelpful default toString, because <tt>substituted</tt> might have passwords in it.
   * If you really want to see the expurgated password, then use <tt>getSubstituted().toString()</tt>.
   */
  @Override
  public String toString()
  {
    return super.toString();
  }
}
