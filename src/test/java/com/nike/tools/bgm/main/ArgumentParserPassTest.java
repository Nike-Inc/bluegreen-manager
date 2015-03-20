package com.nike.tools.bgm.main;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ArgumentParserPassTest
{
  private ArgumentParser argumentParser;
  private String[] args;
  private String expectedJobName;
  private String[][] expectedParameters;

  public ArgumentParserPassTest(String cmdline, String expectedJobName, String[][] expectedParameters)
  {
    this.argumentParser = new ArgumentParser();
    this.args = cmdline.split("\\s+");
    this.expectedJobName = expectedJobName;
    this.expectedParameters = expectedParameters;
    System.out.println("Argument test: " + Arrays.toString(args));
  }

  @Parameters
  public static Collection<Object[]> goodData()
  {
    return Arrays.asList(new Object[][] {
        {
            "job1",
            "job1", new String[][] { }
        },
        {
            "job1 --param1",
            "job1", new String[][] { new String[] { "param1" } }
        },
        {
            "job1 --param1 val1A",
            "job1", new String[][] { new String[] { "param1", "val1A" } }
        },
        {
            "job1 --param1 val1A val1B",
            "job1", new String[][] { new String[] { "param1", "val1A", "val1B" } }
        },
        {
            "job1 --param1 --param2",
            "job1", new String[][] { new String[] { "param1" }, new String[] { "param2" } }
        },
        {
            "job1 --param1 --param2 val2A",
            "job1", new String[][] { new String[] { "param1" }, new String[] { "param2", "val2A" } }
        },
        {
            "job1 --param1 val1A --param2 val2A",
            "job1", new String[][] { new String[] { "param1", "val1A" }, new String[] { "param2", "val2A" } }
        },
        {
            "job1 --param1 --param2 val2A val2B",
            "job1", new String[][] { new String[] { "param1" }, new String[] { "param2", "val2A", "val2B" } }
        },
        {
            "job1 --param1 val1A val1B --param2",
            "job1", new String[][] { new String[] { "param1", "val1A", "val1B" }, new String[] { "param2" } }
        }
    });
  }

  @Test
  public void testParseArgs() throws Exception
  {
    argumentParser.parseArgs(args);

    assertEquals(expectedJobName, argumentParser.getJobName());
    assertEquals(expectedParameters.length, argumentParser.getParameters().size());
    for (int idx = 0; idx < expectedParameters.length; ++idx)
    {
      String[] expectedParameter = expectedParameters[idx];
      List<String> actualParameter = argumentParser.getParameters().get(idx);
      assertEquals("idx " + idx + " in args " + Arrays.toString(args),
          expectedParameter.length, actualParameter.size());
      for (int jdx = 0; jdx < expectedParameter.length; ++jdx)
      {
        assertEquals("idx " + idx + ", jdx " + jdx + " in args " + Arrays.toString(args),
            expectedParameter[jdx], actualParameter.get(jdx));
      }
    }
  }
}