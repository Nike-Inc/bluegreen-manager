package bluegreen.manager.client.app;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Configures a gson parser.
 */
@Component
public class GsonFactory
{
  private GsonBuilder gsonBuilder;

  public GsonFactory()
  {
    gsonBuilder = new GsonBuilder();
  }

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
