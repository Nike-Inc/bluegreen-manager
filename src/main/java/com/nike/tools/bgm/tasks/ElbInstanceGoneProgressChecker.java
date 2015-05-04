package com.nike.tools.bgm.tasks;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.nike.tools.bgm.client.aws.ElbClient;
import com.nike.tools.bgm.utils.ProgressChecker;

/**
 * Knows how to check progress of an EC2 instance deregistering from an ELB, by looking at the ELB's described list of
 * instances and declaring "done" when the instance is gone from the list.
 * <p/>
 * Assumes there will always be another instance left in the ELB after this one is removed, so it would be an error if
 * we found an empty list of instances.
 * <p/>
 * Result is "true" when the deregistered instance is gone from the ELB.
 */
public class ElbInstanceGoneProgressChecker implements ProgressChecker<Boolean>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ElbInstanceGoneProgressChecker.class);

  private String elbName;
  private String ec2InstanceId;
  private String logContext;
  private ElbClient elbClient;
  private boolean done;
  private Boolean result;

  public ElbInstanceGoneProgressChecker(String elbName,
                                        String ec2InstanceId,
                                        String logContext,
                                        ElbClient elbClient)
  {
    this.elbName = elbName;
    this.ec2InstanceId = ec2InstanceId;
    this.logContext = logContext;
    this.elbClient = elbClient;
  }

  @Override
  public String getDescription()
  {
    return "ELB Instance Gone for elb '" + elbName + "', ec2 instance '" + ec2InstanceId + "'";
  }

  /**
   * Initial check calls elbClient to describe the ELB, same as the followup checks, because the initial
   * deregistration call does not return an ELB description.
   */
  @Override
  public void initialCheck()
  {
    LoadBalancerDescription loadBalancerDescription = elbClient.describeLoadBalancer(elbName);
    checkInstanceRemoval(loadBalancerDescription);
    LOGGER.debug(logContext + "Initial ELB instance list: " + summarizeInstances(loadBalancerDescription.getInstances()));
  }

  @Override
  public void followupCheck(int waitNum)
  {
    LoadBalancerDescription loadBalancerDescription = elbClient.describeLoadBalancer(elbName);
    checkInstanceRemoval(loadBalancerDescription);
    LOGGER.debug(logContext + "ELB instance list after wait#" + waitNum + ": " + summarizeInstances(loadBalancerDescription.getInstances()));
  }

  /**
   * Sanity checks the LB description, and checks for done-ness.
   */
  private void checkInstanceRemoval(LoadBalancerDescription loadBalancerDescription)
  {
    if (!StringUtils.equals(elbName, loadBalancerDescription.getLoadBalancerName()))
    {
      throw new IllegalStateException(logContext + "We requested description of ELB '" + elbName
          + "' but response is for '" + loadBalancerDescription.getLoadBalancerName() + "'");
    }
    if (CollectionUtils.isEmpty(loadBalancerDescription.getInstances()))
    {
      throw new IllegalStateException("ELB '" + elbName + "' has zero instances");
    }
    if (instanceIsGoneFromList(loadBalancerDescription.getInstances()))
    {
      LOGGER.info("ELB '" + elbName + "' list of instances shows '" + ec2InstanceId + "' is gone");
      done = true;
      result = true;
    }
  }

  /**
   * True if ec2InstanceId is not in the input list.
   */
  private boolean instanceIsGoneFromList(List<Instance> instances)
  {
    if (instances == null)
    {
      throw new IllegalArgumentException();
    }
    for (Instance instance : instances)
    {
      if (StringUtils.equals(instance.getInstanceId(), ec2InstanceId))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a one-line, comma-separated string of the input instance list.  Health status info is not available.
   * <p/>
   * Example: "i-123456, i-234567"
   */
  private String summarizeInstances(List<Instance> instances)
  {
    return StringUtils.join(instances, ", ");
  }

  @Override
  public boolean isDone()
  {
    return done;
  }

  @Override
  public Boolean getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns null.
   */
  @Override
  public Boolean timeout()
  {
    LOGGER.error("Timeout before ELB Instance Gone");
    return null;
  }
}
