package com.nike.tools.bgm.substituter;

/**
 * Makes string substitutions of %{..} variables, using the blue/green data model (and possibly other data sources).
 * Derived classes should define a substitution map.
 * <p/>
 * Can't support '${..}' since Spring already substitutes that in properties file.
 */
public interface StringSubstituter
{
  /**
   * Loads datamodel entities, in preparation for substitution.
   */
  void loadDataModel();

  /**
   * Substitutes %{..} variables in a <tt>command</tt> string.
   */
  String substituteVariables(String command);
}
