package com.nike.tools.bgm.client.app;

/**
 * Represents the results of database discovery, returned by a bluegreen application.
 */
public class DiscoveryResult implements Lockable
{
  private PhysicalDatabase physicalDatabase;

  private boolean lockError;

  private String discoveryError;

  public DiscoveryResult()
  {
  }

  public DiscoveryResult(PhysicalDatabase physicalDatabase, boolean lockError, String discoveryError)
  {
    this.physicalDatabase = physicalDatabase;
    this.lockError = lockError;
    this.discoveryError = discoveryError;
  }

  public PhysicalDatabase getPhysicalDatabase()
  {
    return physicalDatabase;
  }

  public void setPhysicalDatabase(PhysicalDatabase physicalDatabase)
  {
    this.physicalDatabase = physicalDatabase;
  }

  @Override
  public boolean isLockError()
  {
    return lockError;
  }

  public void setLockError(boolean lockError)
  {
    this.lockError = lockError;
  }

  public String getDiscoveryError()
  {
    return discoveryError;
  }

  public void setDiscoveryError(String discoveryError)
  {
    this.discoveryError = discoveryError;
  }
}
