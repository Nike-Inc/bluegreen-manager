package bluegreen.manager.tasks;

import bluegreen.manager.client.app.DbFreezeMode;

/**
 * Values that distinguish the freeze and thaw transition types.
 */
public class TransitionParameters
{
  /**
   * freeze or thaw, for printing messages.
   */
  private String verb;

  /**
   * Modes the application can be in to legitimately start this transition.
   */
  private DbFreezeMode[] allowedStartModes;

  /**
   * Mode we expect to be in for a while during the transition.
   */
  private DbFreezeMode transitionalMode;

  /**
   * Mode we hope to get to when transition is done.
   */
  private DbFreezeMode destinationMode;

  /**
   * Mode we would expect to end up in if there is a transition error.
   */
  private DbFreezeMode transitionErrorMode;

  /**
   * RESTful resource method name to request the transition.
   */
  private String transitionMethodPath;

  public TransitionParameters(String verb,
                              DbFreezeMode[] allowedStartModes,
                              DbFreezeMode transitionalMode,
                              DbFreezeMode destinationMode,
                              DbFreezeMode transitionErrorMode, String transitionMethodPath)
  {
    this.verb = verb;
    this.allowedStartModes = allowedStartModes;
    this.transitionalMode = transitionalMode;
    this.destinationMode = destinationMode;
    this.transitionErrorMode = transitionErrorMode;
    this.transitionMethodPath = transitionMethodPath;
  }

  public String getVerb()
  {
    return verb;
  }

  public DbFreezeMode[] getAllowedStartModes()
  {
    return allowedStartModes;
  }

  public DbFreezeMode getTransitionalMode()
  {
    return transitionalMode;
  }

  public DbFreezeMode getDestinationMode()
  {
    return destinationMode;
  }

  public DbFreezeMode getTransitionErrorMode()
  {
    return transitionErrorMode;
  }

  public String getTransitionMethodPath()
  {
    return transitionMethodPath;
  }
}
