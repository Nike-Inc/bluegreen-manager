package bluegreen.manager.client.aws;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RdsSnapshotBluegreenIdTest
{
  @Test
  public void testToString()
  {
    assertEquals("bluegreen9theEnv9logicaldb9physicaldb",
        new RdsSnapshotBluegreenId("theEnv", "logicaldb", "physicaldb").toString());
  }
}