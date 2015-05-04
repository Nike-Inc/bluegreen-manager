package com.nike.tools.bgm.client.aws;

/**
 * Represents the possible string names of a {@link com.amazonaws.services.ec2.model.Filter} passed to an
 * EC2 instance request.
 * <p/>
 * Since the full list of names is quite long, this only includes the ones we're using so far.
 */
public enum Ec2InstanceFilter
{
  PRIVATE_IP_ADDRESS;

  /**
   * Returns name as lowercase and with hyphens and dots.
   * <p/>
   * Converts single underscore to hyphen, and double underscore to dot.
   */
  @Override
  public String toString()
  {
    return name().toLowerCase().replace("__", ".").replace('_', '-');
  }
}
