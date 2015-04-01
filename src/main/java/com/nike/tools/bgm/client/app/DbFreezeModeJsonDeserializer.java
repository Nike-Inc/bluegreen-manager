package com.nike.tools.bgm.client.app;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Deserializes an element like <tt>{'printable':'Normal','transition':'blah','code':'NORMAL'}</tt> to a DbFreezeMode.
 * <p/>
 * Reads the <tt>code</tt>, ignores the rest.
 */
public class DbFreezeModeJsonDeserializer implements JsonDeserializer<DbFreezeMode>
{
  @Override
  public DbFreezeMode deserialize(JsonElement json,
                                  Type typeOfT,
                                  JsonDeserializationContext context) throws JsonParseException
  {
    DbFreezeMode mode = null;
    if (json.isJsonObject())
    {
      JsonObject jsonObject = json.getAsJsonObject();
      JsonElement codeElement = jsonObject.get("code");
      if (codeElement != null)
      {
        String code = codeElement.getAsString();
        mode = DbFreezeMode.fromCode(code);
      }
    }
    if (mode == null)
    {
      throw new JsonParseException("Could not deserialize json string as DbFreezeMode: " + json);
    }
    return mode;
  }
}
