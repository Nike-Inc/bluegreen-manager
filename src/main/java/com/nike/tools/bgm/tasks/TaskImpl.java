package com.nike.tools.bgm.tasks;

/**
 * Implementation that has a position and a name.
 * Subclasses will know how to process the task.
 */
public abstract class TaskImpl implements Task
{
  /**
   * The task's ordinal position within its job.
   */
  private int position;

  /**
   * The task name (class simple name).
   */
  private String name;

  /**
   * This would be a TaskImpl constructor except mockito can't mock the argument.
   * <p/>
   * Derived classes should override init() and add any additional post-construct activities that should happen
   * immediately, before tasks start executing.
   */
  protected void init(int position)
  {
    this.position = position;
    this.name = getClass().getSimpleName();
  }

  /**
   * Returns a tiny string pointing out if we're in noop, for logging clarity.
   */
  protected String noopRemark(boolean noop)
  {
    return noop ? " (noop)" : "";
  }

  @Override
  public int getPosition()
  {
    return position;
  }

  @Override
  public String getName()
  {
    return name;
  }
}
