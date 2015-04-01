package com.nike.tools.bgm.client.app;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Configures a gson parser.
 */
public class GsonFactory
{
  @Autowired
  private GsonBuilder gsonBuilder;

  /**
   * Configures gson according to the needs of known classes in this project.
   */
  public Gson makeGson()
  {
    gsonBuilder.registerTypeAdapter(DbFreezeMode.class, new DbFreezeModeJsonDeserializer());
    return gsonBuilder.create();
  }

  //Test purposes only
  void setGsonBuilder(GsonBuilder gsonBuilder)
  {
    this.gsonBuilder = gsonBuilder;
  }
}
