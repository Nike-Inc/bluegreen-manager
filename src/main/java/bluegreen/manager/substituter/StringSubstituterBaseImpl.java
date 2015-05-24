package bluegreen.manager.substituter;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Knows how to substitute keys with values in a command string, using a map of substitutions (provided by derived
 * the class) and system environment variables.
 * <p/>
 * %{..} is for normal variables, %{{..}} is for passwords that should be expurgated in log output.
 */
public abstract class StringSubstituterBaseImpl implements StringSubstituter
{
  protected Map<String, String> substitutions; //Set by derived classes

  /**
   * Performs all variable substitutions on the input <tt>template</tt>.
   */
  @Override
  public SubstituterResult substituteVariables(String template)
  {
    if (substitutions == null)
    {
      throw new IllegalStateException("Need to initialize substitutions first");
    }
    if (StringUtils.isBlank(template))
    {
      throw new IllegalArgumentException("Command template is blank");
    }
    SubstituterResult result = new SubstituterResult(template, template);
    substituteWithMap(result, substitutions);
    substituteWithMap(result, System.getenv()); //e.g. CWD, HOME, USER.  Loses to normal substitutions in case of name conflict.
    return result;
  }

  /**
   * Replaces variables in the <tt>ongoingResult</tt> using key/value pairs from the <tt>map</tt>.
   * <p/>
   *
   * @param map Specifies key/value substitutions, where the key does NOT include the percent sign and curly braces.
   */
  private void substituteWithMap(SubstituterResult ongoingResult, Map<String, String> map)
  {
    if (map != null)
    {
      String substituted = ongoingResult.getSubstituted(); //Never null.
      String expurgated = ongoingResult.getExpurgated(); //Never null.
      for (Map.Entry<String, String> entry : map.entrySet())
      {
        substituted = substitute(substituted, entry.getKey(), entry.getValue());
        expurgated = expurgate(expurgated, entry.getKey(), entry.getValue());
      }
      ongoingResult.setSubstituted(substituted);
      ongoingResult.setExpurgated(expurgated);
    }
  }

  /**
   * Performs a key/value substitution on the template, returning the result.
   * <p/>
   * Substitutes the actual value for both regular and expurgated variable expressions.
   * <p/>
   * Template exits unchanged if the key is not found.
   */
  private String substitute(String template, String key, String value)
  {
    String substituted = StringUtils.replace(template, OPEN_SUBSTITUTE + key + CLOSE_SUBSTITUTE, value);
    return StringUtils.replace(substituted, OPEN_EXPURGATE + key + CLOSE_EXPURGATE, value);
  }

  /**
   * Performs a key/value substitution on the template, returning the result.
   * <p/>
   * Substitutes the actual value for regular variable expressions, and substitutes a bleep for expurgated variable
   * expressions.
   * <p/>
   * Template exits unchanged if the key is not found.
   */
  private String expurgate(String template, String key, String value)
  {
    String substituted = StringUtils.replace(template, OPEN_SUBSTITUTE + key + CLOSE_SUBSTITUTE, value);
    return StringUtils.replace(substituted, OPEN_EXPURGATE + key + CLOSE_EXPURGATE, BLEEP);
  }

}
