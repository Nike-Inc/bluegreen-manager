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
   * "assign" is the activity of saving task arguments.  This would live in the constructor except for the annoying
   * limitation of our mock library mockito which can't mock value types like String and int.
   */
  protected void assign(int position)
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
