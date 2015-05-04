package com.nike.tools.bgm.client.aws;

import java.util.Arrays;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;

/**
 * Sends commands to Amazon ElasticLoadBalancing.
 * <p/>
 * All methods here communicate with Amazon and use a StopWatch.
 */
public class ElbClient
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ElbClient.class);

  /**
   * Synchronous client, requests will block til done.
   */
  private AmazonElasticLoadBalancingClient awsElbClient;

  public ElbClient(AmazonElasticLoadBalancingClient awsElbClient)
  {
    this.awsElbClient = awsElbClient;
  }

  /**
   * Requests registration of the ec2 instance with the ELB.
   * <p/>
   * After calling here, you need to call DescribeLoadBalancers or DescribeInstanceHealth to see if registration is
   * complete.
   */
  public void registerInstance(String elbName, String ec2InstanceId)
  {
    LOGGER.debug("registerInstancesWithLoadBalancer(elbName: " + elbName + ", ec2InstanceId: " + ec2InstanceId + ")");
    assertNonBlankArgs(elbName, ec2InstanceId);
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest();
      request.setLoadBalancerName(elbName);
      request.setInstances(Arrays.asList(new Instance(ec2InstanceId)));
      awsElbClient.registerInstancesWithLoadBalancer(request);
      //Currently not doing anything with the RegisterInstancesWithLoadBalancerResult
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("registerInstancesWithLoadBalancer time elapsed " + stopWatch);
    }
  }

  /**
   * Requests deregistration of the ec2 instance from the ELB.
   * <p/>
   * After calling here, you need to call DescribeLoadBalancers to see if registration is complete.
   */
  public void deregisterInstance(String elbName, String ec2InstanceId)
  {
    LOGGER.debug("deregisterInstancesFromLoadBalancer(elbName: " + elbName + ", ec2InstanceId: " + ec2InstanceId + ")");
    assertNonBlankArgs(elbName, ec2InstanceId);
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest();
      request.setLoadBalancerName(elbName);
      request.setInstances(Arrays.asList(new Instance(ec2InstanceId)));
      awsElbClient.deregisterInstancesFromLoadBalancer(request);
      //Currently not doing anything with the DeregisterInstancesFromLoadBalancerResult
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("deregisterInstancesFromLoadBalancer time elapsed " + stopWatch);
    }
  }

  /**
   * Checks the instance health of the ec2 instance in the given ELB.
   */
  public InstanceState describeInstanceHealth(String elbName, String ec2InstanceId)
  {
    LOGGER.debug("describeInstanceHealth(elbName: " + elbName + ", ec2InstanceId: " + ec2InstanceId + ")");
    assertNonBlankArgs(elbName, ec2InstanceId);
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest();
      request.setLoadBalancerName(elbName);
      request.setInstances(Arrays.asList(new Instance(ec2InstanceId)));
      DescribeInstanceHealthResult result = awsElbClient.describeInstanceHealth(request);
      if (result == null || CollectionUtils.isEmpty(result.getInstanceStates()))
      {
        throw new RuntimeException("ELB '" + elbName + "' didn't match instance id '" + ec2InstanceId + "'");
      }
      else if (result.getInstanceStates().size() > 1)
      {
        LOGGER.warn("Expected 1 instance state for instance id '" + ec2InstanceId + "' in elb '" + elbName + "', found "
            + result.getInstanceStates().size());
      }
      return result.getInstanceStates().get(0);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("describeInstanceHealth time elapsed " + stopWatch);
    }
  }

  /**
   * Returns a description of the named ELB.
   */
  public LoadBalancerDescription describeLoadBalancer(String elbName)
  {
    LOGGER.debug("describeLoadBalancers(elbName: " + elbName + ")");
    if (StringUtils.isBlank(elbName))
    {
      throw new IllegalArgumentException("Blank elbName");
    }
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
      request.setLoadBalancerNames(Arrays.asList(elbName));
      DescribeLoadBalancersResult result = awsElbClient.describeLoadBalancers(request);
      if (result == null || CollectionUtils.isEmpty(result.getLoadBalancerDescriptions()))
      {
        throw new RuntimeException("ELB '" + elbName + "' was not found");
      }
      else if (result.getLoadBalancerDescriptions().size() > 1)
      {
        LOGGER.warn("Expected 1 ELB description for elb name '" + elbName + "', found "
            + result.getLoadBalancerDescriptions().size());
      }
      return result.getLoadBalancerDescriptions().get(0);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("describeLoadBalancers time elapsed " + stopWatch);
    }
  }

  private void assertNonBlankArgs(String elbName, String ec2InstanceId)
  {
    if (StringUtils.isBlank(elbName))
    {
      throw new IllegalArgumentException("Blank elbName");
    }
    if (StringUtils.isBlank(ec2InstanceId))
    {
      throw new IllegalArgumentException("Blank ec2InstanceId");
    }
  }
}
