package com.nike.tools.bgm.client.app;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests the GsonFactory but also the custom Gson deserializer.
 */
public class GsonFactoryTest
{
  private static final String JSON_DB_FREEZE_PROGRESS = "{'mode':{'printable':'Normal','transition':'blah','code':'NORMAL'}, 'username':'charlie', 'startTime':'12pm', 'endTime':'1pm', 'scannersAwaitingTermination':null, 'lockError':false, 'transitionError':null}";

  private Gson gson; //Final class, mockito cannot mock

  @Before
  public void setUp()
  {
    GsonFactory gsonFactory = new GsonFactory();
    gsonFactory.setGsonBuilder(new GsonBuilder());
    gson = gsonFactory.makeGson();
  }

  /**
   * Tests gson deserialization of DbFreezeProgress.
   * <p/>
   * Implicitly tests DbFreezeModeJsonDeserializer.
   */
  @Test
  public void testGson_DbFreezeProgress()
  {
    Lockable response = gson.fromJson(JSON_DB_FREEZE_PROGRESS, DbFreezeProgress.class);
    assertFalse(response.isLockError());
    assertEquals(DbFreezeMode.NORMAL, ((DbFreezeProgress) response).getMode());
  }

}