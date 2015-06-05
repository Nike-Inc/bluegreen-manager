package bluegreen.manager.substituter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Prepares a string substituter with a supplied map, and does not need datamodel entity values.
 */
@Lazy
@Component
@Scope("prototype")
public class ZeroEnvStringSubstituter extends StringSubstituterBaseImpl
{
  private Map<String, String> extraSubstitutions;

  public ZeroEnvStringSubstituter(Map<String, String> extraSubstitutions)
  {
    this.extraSubstitutions = extraSubstitutions;
  }

  @Override
  public void loadDataModel()
  {
    prepareSubstitutions();
  }

  private void prepareSubstitutions()
  {
    substitutions = new HashMap<String, String>();
    if (extraSubstitutions != null)
    {
      substitutions.putAll(extraSubstitutions);
    }
  }

}
