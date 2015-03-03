package com.nike.tools.bgm.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BlueGreenManager
{
  private static Logger LOGGER = LoggerFactory.getLogger(BlueGreenManager.class);

  @Autowired
  private Widget widget;

  public Widget getWidget()
  {
    return widget;
  }

  public void setWidget(Widget widget)
  {
    this.widget = widget;
  }

  public static void main(String[] args)
  {
    ApplicationContext context =
        new ClassPathXmlApplicationContext(new String[] { "applicationContext/main.xml" });

    BlueGreenManager blueGreenManager = context.getBean(BlueGreenManager.class);
    LOGGER.info("Hello {}!", blueGreenManager.getWidget().getName());
  }
}
