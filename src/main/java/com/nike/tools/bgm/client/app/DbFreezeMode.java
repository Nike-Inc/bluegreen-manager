package com.nike.tools.bgm.client.app;

/**
 * Represents the status of an application's ability to write to a database.
 */
public enum DbFreezeMode
{
  NORMAL,
  FLUSHING,
  FLUSH_ERROR,
  FROZEN,
  THAW,
  THAW_ERROR;
}
