package com.nike.tools.bgm.utils;

public class HashUtil
{
  /**
   * Generates a jdk hashcode from a long value.
   */
  public static int hashId(long id)
  {
    final int prime = 31;
    int hash = 17;
    hash = hash * prime + ((int) (id ^ (id >>> 32)));
    return hash;
  }
}
