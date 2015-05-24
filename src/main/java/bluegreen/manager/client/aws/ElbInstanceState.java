package bluegreen.manager.client.aws;

import org.apache.commons.lang3.StringUtils;

/**
 * State of ec2 instance health, reported by an ELB.
 * <p/>
 * There are three possible values, according to http://docs.aws.amazon.com/ElasticLoadBalancing/latest/APIReference/API_InstanceState.html.
 */
public enum ElbInstanceState
{
  IN_SERVICE("InService"),
  OUT_OF_SERVICE("OutOfService"),
  UNKNOWN("Unknown");

  private String name;

  ElbInstanceState(String name)
  {
    this.name = name;
  }

  /**
   * True if the given string matches the name of this enum member.
   */
  public boolean equalsString(String str)
  {
    return StringUtils.equals(name, str);
  }

  /**
   * Returns name.
   */
  @Override
  public String toString()
  {
    return name;
  }
}
