package com.nike.tools.bgm.main;

import org.junit.Before;
import org.junit.Test;

public class ArgumentParserFailTest
{
  private ArgumentParser argumentParser;

  @Before
  public void setUp()
  {
    argumentParser = new ArgumentParser();
  }

  @Test(expected = CmdlineException.class)
  public void testNoArgs()
  {
    argumentParser.parseArgs(new String[] { });
  }

  @Test(expected = CmdlineException.class)
  public void testParamNeedsHyphens()
  {
    argumentParser.parseArgs(new String[] { "job1", "paramName" });
  }
}
