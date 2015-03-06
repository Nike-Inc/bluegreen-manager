package com.nike.tools.bgm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.dao.EnvironmentDAO;
import com.nike.tools.bgm.model.domain.Environment;

@Component
public class BlueGreenManager
{
  private static Logger LOGGER = LoggerFactory.getLogger(BlueGreenManager.class);

  @Autowired
  private Widget widget;

  @Autowired
  private EnvironmentDAO environmentDAO;

  public Widget getWidget()
  {
    return widget;
  }

  public void setWidget(Widget widget)
  {
    this.widget = widget;
  }

  private void messWithEnvironments()
  {
    List<Environment> environments = environmentDAO.findAll();
    LOGGER.info("environments: " + (environments == null ? "null" : environments.size()));
  }

  public static void main(String[] args)
  {
    ApplicationContext context =
        new ClassPathXmlApplicationContext(new String[] { "applicationContext/main.xml" });

    BlueGreenManager blueGreenManager = context.getBean(BlueGreenManager.class);
    LOGGER.info("Hello {}!", blueGreenManager.getWidget().getName());

    blueGreenManager.messWithEnvironments();
  }
}
