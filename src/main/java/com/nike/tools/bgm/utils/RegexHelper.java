package com.nike.tools.bgm.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class RegexHelper
{
  /**
   * True if the specified pattern is found in the multi-line text.
   */
  public boolean matcherFind(String text, Pattern pattern)
  {
    if (text != null && pattern != null)
    {
      for (String line : parseLines(text))
      {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find())
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the numbered capture group in the first line of output where the pattern is found.
   * <p/>
   * Or null if matcher cannot find it.
   */
  public String matcherFirstCapture(String text, Pattern pattern, int groupNum)
  {
    for (String line : parseLines(text))
    {
      Matcher matcher = pattern.matcher(line);
      if (matcher.find())
      {
        String value = matcher.group(groupNum);
        if (StringUtils.isNotBlank(value))
        {
          return value;
        }
      }
    }
    return null;
  }

  /**
   * Parses a multi-line text string into an array of single lines.
   * <p/>
   * e.g. Parses "Hello\nWorld\n" to ("Hello", "World")
   */
  private String[] parseLines(String text)
  {
    return text.split("[\\r\\n]+");
  }

}
