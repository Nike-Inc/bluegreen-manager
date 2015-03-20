package com.nike.tools.bgm.tasks;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.TaskStatus;

@Lazy
@Component
public class WorldTask extends TaskImpl
{
  private String worldValue;

  public WorldTask(int position, String worldValue)
  {
    super(position);
    this.worldValue = worldValue;
  }

  @Override
  public TaskStatus process(boolean noop)
  {
    System.out.println("*** WORLD " + worldValue + " ***");
    return TaskStatus.DONE;
  }
}
