package com.nike.tools.bgm.client.app;

/**
 * Represents the status of an application's ability to write to a database.
 */
public enum DbFreezeMode
{
  /**
   * Database is writable and is operating normally.
   * <p/>
   * This is the only state where new attempts to write to the database are ok.
   * In all other modes, new writes will be an error.
   */
  NORMAL("NORMAL", "Normal", "Enter dbfreeze"),

  /**
   * Application is in the process of flushing the pending write operations to the database.
   * Scanner threads are in the process of terminating and being removed from the scheduler.
   */
  FLUSHING("FLUSHING", "Flushing", null),

  /**
   * Application failed to flush writes and/or terminate scanners.
   */
  FLUSH_ERROR("FLUSH_ERROR", "Flush Error", "Try again to enter dbfreeze"),

  /**
   * Database is frozen into read-only mode (ignoring RDS 'super' privileges).
   * No scanner threads are running or scheduled.
   */
  FROZEN("FROZEN", "Frozen", "Exit dbfreeze"),

  /**
   * Database is in the process of exiting read-only mode.  This entails adding scanner threads
   * to a scheduler and executing some db statements.
   */
  THAW("THAW", "Thaw", null),

  /**
   * Application failed to re-enable write mode and/or restart write operations.
   * <p/>
   * THAW_ERROR would probably be an in-memory state only, since it mainly implies the database still thinks state=FROZEN.
   */
  THAW_ERROR("THAW_ERROR", "Thaw Error", "Try again to exit dbfreeze");

  /**
   * Internal code for html values or database values which the user will not see.
   */
  private String code;

  /**
   * User visible value that is pretty and user-friendly.
   */
  private String printable;

  /**
   * Description of a permissible transition away from this mode.
   */
  private String transition;

  DbFreezeMode(String code, String printable, String transition)
  {
    this.code = code;
    this.printable = printable;
    this.transition = transition;
  }

  public String getCode()
  {
    return code;
  }

  public String getPrintable()
  {
    return printable;
  }

  public String getTransition()
  {
    return transition;
  }

  /**
   * Returns true if this is a "transitional mode", i.e. a transition from one steady-state mode to another.
   * <p/>
   * FLUSHING and THAW are the two transitional modes.
   */
  public boolean isTransitional()
  {
    return transition == null;
  }

  /**
   * Returns the DbFreezeMode that follows the current mode.
   */
  public DbFreezeMode next()
  {
    switch (this)
    {
      case NORMAL:
        return FLUSHING;
      case FLUSHING:
        return FROZEN;
      case FLUSH_ERROR:
        return FLUSHING;
      case FROZEN:
        return THAW;
      case THAW:
        return NORMAL;
      case THAW_ERROR:
        return THAW;
      default:
        throw new IllegalStateException();
    }
  }

  public static DbFreezeMode fromCode(String code)
  {
    if (code != null)
    {
      for (DbFreezeMode mode : values())
      {
        if (code.equals(mode.code))
        {
          return mode;
        }
      }
    }
    return null;
  }
}
