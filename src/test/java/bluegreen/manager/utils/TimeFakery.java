package bluegreen.manager.utils;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Some fake times for test purposes.
 */
public class TimeFakery
{
  public static String START_TIME_STRING = "2015-01-01 12:30:00 -0800";
  public static Date START_TIME = DateHelper.tzstringToDate(START_TIME_STRING);
  public static Timestamp START_TIMESTAMP = new Timestamp(START_TIME.getTime());
}
