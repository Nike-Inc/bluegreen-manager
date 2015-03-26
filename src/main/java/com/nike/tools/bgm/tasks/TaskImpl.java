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
   */
  protected void init(int position)
  {
    this.position = position;
    this.name = getClass().getSimpleName();
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
