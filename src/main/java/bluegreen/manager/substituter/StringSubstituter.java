package bluegreen.manager.substituter;

/**
 * Makes string substitutions of %{..} and %{{..}} variables in a command string.
 * Derived classes should define a substitution map.
 * <p/>
 * Can't support '${..}' since Spring already substitutes that in properties file.
 * <p/>
 * %{..} is for normal variables, %{{..}} is for passwords that should be expurgated in log output.
 */
public interface StringSubstituter
{
  String BLEEP = "XXXXX";
  String OPEN_SUBSTITUTE = "%{";
  String CLOSE_SUBSTITUTE = "}";
  String OPEN_EXPURGATE = "%{{";
  String CLOSE_EXPURGATE = "}}";

  /**
   * Loads datamodel entities, in preparation for substitution.
   */
  void loadDataModel();

  /**
   * Substitutes %{..} and %{{..}} variables in a <tt>command</tt> string.
   */
  SubstituterResult substituteVariables(String command);
}
