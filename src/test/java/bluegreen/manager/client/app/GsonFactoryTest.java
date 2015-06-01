package bluegreen.manager.client.app;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Tests the GsonFactory but also the custom Gson deserializer.
 */
public class GsonFactoryTest
{
  private static final String JSON_DB_FREEZE_PROGRESS = "{'mode':{'printable':'Normal','transition':'blah','code':'NORMAL'}, 'username':'charlie', 'startTime':'12pm', 'endTime':'1pm', 'lockError':false, 'transitionError':null}";
  private static final String JSON_DB_FREEZE_PROGRESS_MISSING = "{'mode':{'printable':'Normal','code':'NORMAL'}, 'username':'charlie', 'startTime':'12pm', 'lockError':false, 'transitionError':null}";
  private static final String JSON_DB_FREEZE_PROGRESS_EXTRANEOUS = "{'mode':{'printable':'Normal','transition':'blah','code':'NORMAL'}, 'username':'charlie', 'startTime':'12pm', 'endTime':'1pm', 'extraneousField':12345, 'lockError':false, 'transitionError':null}";

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

  /**
   * Tests gson deserialization of DbFreezeProgress, when JSON is missing some fields.
   * Asserts that the missing fields get default values.
   */
  @Test
  public void testGson_DbFreezeProgress_MissingFields()
  {
    DbFreezeProgress response = gson.fromJson(JSON_DB_FREEZE_PROGRESS_MISSING, DbFreezeProgress.class);
    assertNull(response.getEndTime());
    assertEquals(DbFreezeMode.NORMAL, response.getMode());
  }

  /**
   * Tests gson deserialization of DbFreezeProgress, when JSON has an extraneous field.
   * Asserts that the normal fields are deserialized as per usual.
   */
  @Test
  public void testGson_DbFreezeProgress_ExtraneousField()
  {
    DbFreezeProgress response = gson.fromJson(JSON_DB_FREEZE_PROGRESS_EXTRANEOUS, DbFreezeProgress.class);
    assertEquals("charlie", response.getUsername());
    assertEquals("1pm", response.getEndTime());
    assertEquals(DbFreezeMode.NORMAL, response.getMode());
  }

}