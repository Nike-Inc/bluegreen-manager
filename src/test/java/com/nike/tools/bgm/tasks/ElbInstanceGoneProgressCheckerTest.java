package com.nike.tools.bgm.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.nike.tools.bgm.client.aws.ElbClient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElbInstanceGoneProgressCheckerTest
{
  private static final String LOG_CONTEXT = "(Log Context) ";
  private static final int WAIT_NUM = 1;
  private static final String ELB_NAME = "the-load-balancer";
  private static final String ANOTHER_ELB_NAME = "another-load-balancer";
  private static final String EC2_INSTANCE_ID_LEAVING = "i-123456"; //Leaving the ELB
  private static final String EC2_INSTANCE_ID_STAYING = "i-234567"; //Staying in the ELB

  @Mock
  private ElbClient mockElbClient;

  private ElbInstanceGoneProgressChecker progressChecker;

  @Before
  public void makeProgressChecker()
  {
    progressChecker = new ElbInstanceGoneProgressChecker(ELB_NAME, EC2_INSTANCE_ID_LEAVING, LOG_CONTEXT, mockElbClient);
  }

  @Test
  public void testGetDescription()
  {
    assertTrue(StringUtils.isNotBlank(progressChecker.getDescription()));
  }

  /**
   * Test helper - makes a fake LB description.
   */
  private LoadBalancerDescription makeLoadBalancerDescription(String elbName, String... instanceIds)
  {
    LoadBalancerDescription loadBalancerDescription = new LoadBalancerDescription();
    loadBalancerDescription.setLoadBalancerName(elbName);
    List<Instance> instances = new ArrayList<Instance>();
    if (instanceIds != null)
    {
      for (String instanceId : instanceIds)
      {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.add(instance);
      }
    }
    loadBalancerDescription.setInstances(instances);
    return loadBalancerDescription;
  }

  private void setupMock(LoadBalancerDescription fakeLoadBalancerDescription)
  {
    when(mockElbClient.describeLoadBalancer(ELB_NAME)).thenReturn(fakeLoadBalancerDescription);
  }

  /**
   * Fail: aws returns LB description for wrong ELB.
   */
  @Test(expected = IllegalStateException.class)
  public void testInitialCheck_WrongELB()
  {
    setupMock(makeLoadBalancerDescription(ANOTHER_ELB_NAME, EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING));
    progressChecker.initialCheck();
  }

  /**
   * Fail: aws returns LB with zero instances.
   */
  @Test(expected = IllegalStateException.class)
  public void testInitialCheck_ZeroInstances()
  {
    setupMock(makeLoadBalancerDescription(ELB_NAME));
    progressChecker.initialCheck();
  }

  /**
   * When ec2 instance is already gone from the list, checker is done.
   */
  @Test
  public void testInitialCheck_Done()
  {
    setupMock(makeLoadBalancerDescription(ELB_NAME, EC2_INSTANCE_ID_STAYING));
    progressChecker.initialCheck();
    assertTrue(progressChecker.isDone());
    assertTrue(progressChecker.getResult());
  }

  /**
   * When ec2 instance is still in the list, progress must continue.
   */
  @Test
  public void testInitialCheck_NotDone()
  {
    setupMock(makeLoadBalancerDescription(ELB_NAME, EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING));
    progressChecker.initialCheck();
    assertFalse(progressChecker.isDone());
    assertNull(progressChecker.getResult());
  }

  /**
   * Fail: aws returns LB description for wrong ELB.
   */
  @Test(expected = IllegalStateException.class)
  public void testFollowupCheck_WrongELB()
  {
    setupMock(makeLoadBalancerDescription(ANOTHER_ELB_NAME, EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING));
    progressChecker.followupCheck(WAIT_NUM);
  }

  /**
   * Fail: aws returns LB with zero instances.
   */
  @Test(expected = IllegalStateException.class)
  public void testFollowupCheck_ZeroInstances()
  {
    setupMock(makeLoadBalancerDescription(ELB_NAME));
    progressChecker.followupCheck(WAIT_NUM);
  }

  /**
   * When ec2 instance is already gone from the list, checker is done.
   */
  @Test
  public void testFollowupCheck_Done()
  {
    setupMock(makeLoadBalancerDescription(ELB_NAME, EC2_INSTANCE_ID_STAYING));
    progressChecker.followupCheck(WAIT_NUM);
    assertTrue(progressChecker.isDone());
    assertTrue(progressChecker.getResult());
  }

  /**
   * When ec2 instance is still in the list, progress must continue.
   */
  @Test
  public void testFollowupCheck_NotDone()
  {
    setupMock(makeLoadBalancerDescription(ELB_NAME, EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING));
    progressChecker.followupCheck(WAIT_NUM);
    assertFalse(progressChecker.isDone());
    assertNull(progressChecker.getResult());
  }

}