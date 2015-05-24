package bluegreen.manager.model.domain;

/**
 * Status of a task.
 */
public enum TaskStatus
{
  NOOP,
  SKIPPED,
  PROCESSING,
  DONE,
  ERROR;
}
