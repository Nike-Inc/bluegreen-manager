package com.nike.tools.bgm.main;

/**
 * This is what main() returns to the system.
 */
public enum ReturnCode
{
  SUCCESS(0),
  CMDLINE_ERROR(1),
  PROCESSING_ERROR(2);

  private int code;

  ReturnCode(int code)
  {
    this.code = code;
  }

  public int getCode()
  {
    return code;
  }
}
