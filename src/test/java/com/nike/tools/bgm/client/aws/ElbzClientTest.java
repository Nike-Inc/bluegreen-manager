package com.nike.tools.bgm.client.aws;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests error-handling capabilities when mock aws returns unexpected results.
 * <p/>
 * Or for the solely declarative methods, simply tests that the aws client is invoked (not much of a test, really).
 */
public class ElbzClientTest
{
  private static final String ELB_NAME = "my-load-balancer";
  private static final String ANOTHER_ELB_NAME = "another-load-balancer";
  private static final String EC2_INSTANCE_ID = "i-123456";
  private static final String ANOTHER_EC2_INSTANCE_ID = "i-234567";

  private AmazonElasticLoadBalancingClient mockELBClient = mock(AmazonElasticLoadBalancingClient.class);
  private ElbzClient elbzClient = new ElbzClient(mockELBClient);

  /**
   * Proves the client method is called.
   */
  @Test
  public void testRegisterInstance()
  {
    elbzClient.registerInstance(ELB_NAME, EC2_INSTANCE_ID);
    verify(mockELBClient).registerInstancesWithLoadBalancer(any(RegisterInstancesWithLoadBalancerRequest.class));
  }

  /**
   * Proves the client method is called.
   */
  @Test
  public void testDeregisterInstance()
  {
    elbzClient.deregisterInstance(ELB_NAME, EC2_INSTANCE_ID);
    verify(mockELBClient).deregisterInstancesFromLoadBalancer(any(DeregisterInstancesFromLoadBalancerRequest.class));
  }

  /**
   * Fail case: describe request gets result with empty list of instance states.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeInstanceHealth_CantFind()
  {
    setupMock(makeDescribeInstanceHealthResult(null));
    elbzClient.describeInstanceHealth(ELB_NAME, EC2_INSTANCE_ID);
  }

  /**
   * Warn case: describe request gets result with too many instance states.  Returns first one.
   */
  @Test
  public void testDescribeInstanceHealth_FoundTooMany()
  {
    setupMock(makeDescribeInstanceHealthResult(EC2_INSTANCE_ID, ANOTHER_EC2_INSTANCE_ID));
    elbzClient.describeInstanceHealth(ELB_NAME, EC2_INSTANCE_ID);
  }

  /**
   * Pass case: describe request gets result with exactly one instance state.
   */
  @Test
  public void testDescribeInstanceHealth_Pass()
  {
    setupMock(makeDescribeInstanceHealthResult(EC2_INSTANCE_ID));
    elbzClient.describeInstanceHealth(ELB_NAME, EC2_INSTANCE_ID);
  }

  /**
   * Sets up the mock elb client to return a fakeResult for the describe-instance-health call.
   */
  private void setupMock(DescribeInstanceHealthResult fakeResult)
  {
    when(mockELBClient.describeInstanceHealth(any(DescribeInstanceHealthRequest.class))).thenReturn(fakeResult);
  }

  /**
   * Test helper - makes describe result with one or more named instances.
   */
  private DescribeInstanceHealthResult makeDescribeInstanceHealthResult(String... instanceIds)
  {
    DescribeInstanceHealthResult result = new DescribeInstanceHealthResult();
    if (ArrayUtils.isNotEmpty(instanceIds))
    {
      List<InstanceState> instanceStates = new ArrayList<InstanceState>();
      for (String instanceId : instanceIds)
      {
        InstanceState instanceState = new InstanceState();
        instanceState.setInstanceId(instanceId);
        instanceStates.add(instanceState);
      }
      result.setInstanceStates(instanceStates);
    }
    return result;
  }

  /**
   * Fail case: describe request gets result with empty list of LB descriptions.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeLoadBalancer_CantFind()
  {
    setupMock(makeDescribeLoadBalancersResult(null));
    elbzClient.describeLoadBalancer(ELB_NAME);
  }

  /**
   * Warn case: describe request gets result with too many LB descriptions.  Returns first one.
   */
  @Test
  public void testDescribeLoadBalancer_FoundTooMany()
  {
    setupMock(makeDescribeLoadBalancersResult(ELB_NAME, ANOTHER_ELB_NAME));
    elbzClient.describeLoadBalancer(ELB_NAME);
  }

  /**
   * Pass case: describe request gets result with exactly one LB description.
   */
  @Test
  public void testDescribeLoadBalancer_Pass()
  {
    setupMock(makeDescribeLoadBalancersResult(ELB_NAME));
    elbzClient.describeLoadBalancer(ELB_NAME);
  }

  private void setupMock(DescribeLoadBalancersResult fakeResult)
  {
    when(mockELBClient.describeLoadBalancers(any(DescribeLoadBalancersRequest.class))).thenReturn(fakeResult);
  }

  /**
   * Test helper - makes describe result with one or more named LBs.
   */
  private DescribeLoadBalancersResult makeDescribeLoadBalancersResult(String... loadBalancerNames)
  {
    DescribeLoadBalancersResult result = new DescribeLoadBalancersResult();
    if (ArrayUtils.isNotEmpty(loadBalancerNames))
    {
      List<LoadBalancerDescription> list = new ArrayList<LoadBalancerDescription>();
      for (String loadBalancerName : loadBalancerNames)
      {
        LoadBalancerDescription loadBalancerDescription = new LoadBalancerDescription();
        loadBalancerDescription.setLoadBalancerName(loadBalancerName);
        list.add(loadBalancerDescription);
      }
      result.setLoadBalancerDescriptions(list);
    }
    return result;
  }
}