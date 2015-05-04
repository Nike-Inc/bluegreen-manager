package com.nike.tools.bgm.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.nike.tools.bgm.client.aws.Ec2zClient;
import com.nike.tools.bgm.client.aws.Ec2zClientFactory;
import com.nike.tools.bgm.client.aws.ElbzClient;
import com.nike.tools.bgm.client.aws.ElbzClientFactory;
import com.nike.tools.bgm.client.aws.ElbzInstanceState;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.WaiterParameters;

import static com.nike.tools.bgm.client.aws.ElbzInstanceState.IN_SERVICE;
import static com.nike.tools.bgm.client.aws.ElbzInstanceState.OUT_OF_SERVICE;
import static com.nike.tools.bgm.client.aws.ElbzInstanceState.UNKNOWN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FixedElbzFlipEc2zTaskTest
{
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);
  private static final String ELB_NAME = "the-load-balancer";
  private static final String EC2_INSTANCE_ID_LEAVING = "i-123456"; //Leaving the ELB
  private static final String EC2_INSTANCE_ID_STAYING = "i-234567"; //Staying in the ELB

  @InjectMocks
  private FixedElbzFlipEc2zTask fixedElbzFlipEc2zTask;

  @Spy
  protected WaiterParameters fakeWaiterParameters = new WaiterParameters(10L, 10L, 2, 3/*short timeout*/);

  @Mock
  private ThreadSleeper mockThreadSleeper;

  @Mock
  private Ec2zClientFactory mockEc2ClientFactory;

  @Mock
  private ElbzClientFactory mockElbzClientFactory;

  @Mock
  private Ec2zClient mockEc2Client;

  @Mock
  private ElbzClient mockElbzClient;

  @Mock
  private EnvironmentTx mockEnvironmentTx;

  @Mock
  private com.amazonaws.services.ec2.model.Instance mockEc2Instance;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_LIVE_ENV.getEnvName())).thenReturn(FAKE_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_STAGE_ENV.getEnvName())).thenReturn(FAKE_STAGE_ENV);
    fixedElbzFlipEc2zTask.assign(1, FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName(), ELB_NAME);

    when(mockEc2ClientFactory.create()).thenReturn(mockEc2Client);
    when(mockElbzClientFactory.create()).thenReturn(mockElbzClient);
    when(mockEc2Client.describeInstanceByPrivateIPAddress(anyString())).thenReturn(mockEc2Instance);
    when(mockEc2Instance.getInstanceId())
        .thenReturn(EC2_INSTANCE_ID_LEAVING) //First call from findLiveEc2InstanceId
        .thenReturn(EC2_INSTANCE_ID_STAYING);//Second call from findStageEc2InstanceId
  }

  /**
   * Sets up elb instance-health progress check to return a sequence of states.
   */
  private void setupMockHealth(InstanceState[] fakeInstanceStates)
  {
    when(mockElbzClient.describeInstanceHealth(ELB_NAME, EC2_INSTANCE_ID_STAYING))
        .thenReturn(fakeInstanceStates[0], ArrayUtils.subarray(fakeInstanceStates, 1, fakeInstanceStates.length));
  }

  private InstanceState makeInstanceState(String instanceId, ElbzInstanceState elbzInstanceState)
  {
    InstanceState instanceState = new InstanceState();
    instanceState.setInstanceId(instanceId);
    instanceState.setState(elbzInstanceState.toString());
    return instanceState;
  }

  private InstanceState[] makeInstanceStates(String instanceId, ElbzInstanceState... elbzInstanceStates)
  {
    InstanceState[] array = new InstanceState[elbzInstanceStates.length];
    for (int idx = 0; idx < elbzInstanceStates.length; ++idx)
    {
      array[idx] = makeInstanceState(instanceId, elbzInstanceStates[idx]);
    }
    return array;
  }

  /**
   * New ec2 instance ("staying") goes in service on 3rd progress check.
   */
  @Test
  public void testWaitTilEC2InstanceIsInService_Pass()
  {
    setupMockHealth(makeInstanceStates(EC2_INSTANCE_ID_STAYING, OUT_OF_SERVICE, OUT_OF_SERVICE, IN_SERVICE));
    fixedElbzFlipEc2zTask.loadDataModel();
    fixedElbzFlipEc2zTask.waitTilEC2InstanceIsInService(EC2_INSTANCE_ID_STAYING);
    verify(mockElbzClient, times(3)).describeInstanceHealth(ELB_NAME, EC2_INSTANCE_ID_STAYING);
  }

  /**
   * New ec2 instance ("staying") is not in service after 3rd progress check: timeout.
   */
  @Test(expected = RuntimeException.class)
  public void testWaitTilEC2InstanceIsInService_Timeout()
  {
    setupMockHealth(makeInstanceStates(EC2_INSTANCE_ID_STAYING, OUT_OF_SERVICE, OUT_OF_SERVICE, OUT_OF_SERVICE));
    fixedElbzFlipEc2zTask.loadDataModel();
    fixedElbzFlipEc2zTask.waitTilEC2InstanceIsInService(EC2_INSTANCE_ID_STAYING);
  }

  /**
   * Sets up an elb describe-load-balancers progress check to return a sequence of LB descriptions.
   */
  private void setupMockDescription(LoadBalancerDescription[] fakeDescriptions)
  {
    when(mockElbzClient.describeLoadBalancer(ELB_NAME))
        .thenReturn(fakeDescriptions[0], ArrayUtils.subarray(fakeDescriptions, 1, fakeDescriptions.length));
  }

  private LoadBalancerDescription makeLoadBalancerDescription(String... instanceIds)
  {
    LoadBalancerDescription loadBalancerDescription = new LoadBalancerDescription();
    loadBalancerDescription.setLoadBalancerName(ELB_NAME);
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

  private LoadBalancerDescription[] makeLoadBalancerDescriptions(String[]... instanceIdsArray)
  {
    LoadBalancerDescription[] array = new LoadBalancerDescription[instanceIdsArray.length];
    for (int idx = 0; idx < instanceIdsArray.length; ++idx)
    {
      array[idx] = makeLoadBalancerDescription(instanceIdsArray[idx]);
    }
    return array;
  }

  /**
   * Old ec2 instance ("leaving") is gone from the LB on 3rd progress check.
   */
  @Test
  public void testWaitTilEC2InstanceIsDeregistered_Pass()
  {
    setupMockDescription(makeLoadBalancerDescriptions(
        new String[] { EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING },
        new String[] { EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING },
        new String[] { EC2_INSTANCE_ID_STAYING }));
    fixedElbzFlipEc2zTask.loadDataModel();
    fixedElbzFlipEc2zTask.waitTilEC2InstanceIsDeregistered(EC2_INSTANCE_ID_LEAVING);
    verify(mockElbzClient, times(3)).describeLoadBalancer(ELB_NAME);
  }

  /**
   * Old ec2 instance ("leaving") is not yet gone from the LB after 3rd progress check: timeout.
   */
  @Test(expected = RuntimeException.class)
  public void testWaitTilEC2InstanceIsDeregistered_Timeout()
  {
    setupMockDescription(makeLoadBalancerDescriptions(
        new String[] { EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING },
        new String[] { EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING },
        new String[] { EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING }));
    fixedElbzFlipEc2zTask.loadDataModel();
    fixedElbzFlipEc2zTask.waitTilEC2InstanceIsDeregistered(EC2_INSTANCE_ID_LEAVING);
  }

  @Test
  public void testProcess_Noop()
  {
    assertEquals(TaskStatus.NOOP, fixedElbzFlipEc2zTask.process(true));
  }

  /**
   * Sets up mocks so new ec2 inst ("staying") is in-service after three waits, and the old ec2 inst ("leaving")
   * is gone after two waits.
   */
  @Test
  public void testProcess_Done()
  {
    setupMockHealth(makeInstanceStates(EC2_INSTANCE_ID_STAYING, UNKNOWN, OUT_OF_SERVICE, IN_SERVICE));
    setupMockDescription(makeLoadBalancerDescriptions(
        new String[] { EC2_INSTANCE_ID_LEAVING, EC2_INSTANCE_ID_STAYING },
        new String[] { EC2_INSTANCE_ID_STAYING }));

    assertEquals(TaskStatus.DONE, fixedElbzFlipEc2zTask.process(false));

    verify(mockEc2Client, times(2)).describeInstanceByPrivateIPAddress(anyString());
    verify(mockElbzClient).registerInstance(ELB_NAME, EC2_INSTANCE_ID_STAYING);
    verify(mockElbzClient, times(3)).describeInstanceHealth(ELB_NAME, EC2_INSTANCE_ID_STAYING);
    verify(mockElbzClient, times(2)).describeLoadBalancer(ELB_NAME);
  }
}