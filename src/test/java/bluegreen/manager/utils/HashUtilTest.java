package bluegreen.manager.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HashUtilTest
{

  @Test
  public void testHashId_zero() throws Exception
  {
    assertEquals(527, HashUtil.hashId(0L));
  }

  @Test
  public void testHashId_nonzeroId() throws Exception
  {
    assertEquals(1761, HashUtil.hashId(1234L));
  }
}