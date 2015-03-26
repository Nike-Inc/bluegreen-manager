package com.nike.tools.bgm.client.app;

/**
 * Progress object returned from a bluegreen application.
 */
public class DbFreezeProgress implements Lockable
{
  /**
   * Current dbfreeze mode, or null if lockError.
   */
  private DbFreezeMode mode;

  /**
   * SamAccountName of the user who started the current transition, or null if mode is non-transitional.
   */
  private String username;

  /**
   * Date/time in local timezone when current transition started, or null if mode is non-transitional.
   */
  private String startTime;

  /**
   * Date/time in local timezone when last transition ended, or null if mode is currently in transition or mode
   * has not transitioned since startup.
   */
  private String endTime;

  /**
   * Comma-delimited list of scanners awaiting termination.  Should be blank in steady-state modes.
   */
  private String scannersAwaitingTermination;

  /**
   * True if the progress request could not lock the dbfreeze synchronizer, which means another freeze-related
   * thread has the lock (either asking for progress or trying to start a new transition).  If true, the caller
   * should wait and ask again for progress later.
   */
  private boolean lockError;

  /**
   * This field has a value when the current progress request obtained a valid lock but failed to start a
   * new transition.  Field is empty otherwise.
   */
  private String transitionError;

  public DbFreezeMode getMode()
  {
    return mode;
  }

  public void setMode(DbFreezeMode mode)
  {
    this.mode = mode;
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getStartTime()
  {
    return startTime;
  }

  public void setStartTime(String startTime)
  {
    this.startTime = startTime;
  }

  public String getEndTime()
  {
    return endTime;
  }

  public void setEndTime(String endTime)
  {
    this.endTime = endTime;
  }

  public String getScannersAwaitingTermination()
  {
    return scannersAwaitingTermination;
  }

  public void setScannersAwaitingTermination(String scannersAwaitingTermination)
  {
    this.scannersAwaitingTermination = scannersAwaitingTermination;
  }

  @Override
  public boolean isLockError()
  {
    return lockError;
  }

  public void setLockError(boolean lockError)
  {
    this.lockError = lockError;
  }

  public String getTransitionError()
  {
    return transitionError;
  }

  public void setTransitionError(String transitionError)
  {
    this.transitionError = transitionError;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("DbFreezeProgress[");
    sb.append("mode: ");
    sb.append(mode);
    sb.append(", username: ");
    sb.append(username);
    sb.append(", startTime: ");
    sb.append(startTime);
    sb.append(", endTime: ");
    sb.append(endTime);
    sb.append(", scannersAwaitingTermination: ");
    sb.append(scannersAwaitingTermination);
    sb.append(", lockError: ");
    sb.append(lockError);
    sb.append(", transitionError: ");
    sb.append(transitionError);
    sb.append("]");
    return sb.toString();
  }
}
