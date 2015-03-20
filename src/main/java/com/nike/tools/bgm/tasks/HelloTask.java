package com.nike.tools.bgm.tasks;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.TaskStatus;

@Lazy
@Component
public class HelloTask extends TaskImpl
{
  private String helloValue;

  public HelloTask(int position, String helloValue)
  {
    super(position);
    this.helloValue = helloValue;
  }

  @Override
  public TaskStatus process(boolean noop)
  {
    System.out.println("*** HELLO " + helloValue + " ***");
    return TaskStatus.DONE;
  }
}
