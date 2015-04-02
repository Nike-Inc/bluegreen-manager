package com.nike.tools.bgm.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelper
{
  private DateHelper()
  {
    //Do not instantiate me
  }

  /**
   * Converts a string with a timezone to a date.
   * <p/>
   * Hard to know how to name these methods...there are so many possible formats.
   */
  public static Date tzstringToDate(String tzstring)
  {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    Date date;
    try
    {
      date = dateFormat.parse(tzstring);
    }
    catch (ParseException e)
    {
      throw new RuntimeException(e);
    }
    return date;
  }

  /**
   * Formats a date as a string with a timezone.
   * <p/>
   * Hard to know how to name these methods...there are so many possible formats.
   */
  public static String dateToTzstring(Date date)
  {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    if (date == null)
    {
      return null;
    }
    else
    {
      return dateFormat.format(date);
    }
  }
}
