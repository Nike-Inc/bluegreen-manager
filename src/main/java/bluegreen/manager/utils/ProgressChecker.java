package bluegreen.manager.utils;

/**
 * Knows how to check progress on a long-running external operation.
 * Used by a {@link Waiter}.
 */
public interface ProgressChecker<T>
{
  /**
   * A short description of what we're waiting for, suitable for logging.
   */
  String getDescription();

  /**
   * Checks results of the initial command.  Most likely not done yet.
   */
  void initialCheck();

  /**
   * Checks results of the ongoing command, not the first time (waitNum > 0).
   */
  void followupCheck(int waitNum);

  /**
   * True if the latest check has determined that the external operation has reached its natural conclusion.
   * False if still transitional or timeout.
   */
  boolean isDone();

  /**
   * Returns a result object after the operation has reached its natural conclusion.
   */
  T getResult();

  /**
   * Acknowledges a timeout in some fashion, and returns a result object (could be null) that will inform the
   * caller it was a timeout.
   */
  T timeout();
}
