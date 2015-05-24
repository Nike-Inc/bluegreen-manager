package bluegreen.manager.tasks;

import bluegreen.manager.model.domain.TaskStatus;

/**
 * Represents a task in a job sequence.
 */
public interface Task
{
  /**
   * Executes the task (unless noop) and returns a status.
   */
  TaskStatus process(boolean noop);

  /**
   * Returns the task's ordinal position within its job.
   */
  int getPosition();

  /**
   * Returns the task name (class simple name).
   */
  String getName();
}
