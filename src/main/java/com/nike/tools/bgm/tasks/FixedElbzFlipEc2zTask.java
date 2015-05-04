package com.nike.tools.bgm.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.nike.tools.bgm.client.aws.Ec2zClient;
import com.nike.tools.bgm.client.aws.Ec2zClientFactory;
import com.nike.tools.bgm.client.aws.ElbzClient;
import com.nike.tools.bgm.client.aws.ElbzClientFactory;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.Waiter;
import com.nike.tools.bgm.utils.WaiterParameters;

/**
 * "Flips the switch" in the Amazon Elastic Load Balancer, from oldLiveEnv to newLiveEnv, by deregistering old vms and
 * registering new vms in the ELB instance pool.  Leaves the existing ELB fixed in place.  There is a delay in this
 * approach because the AWS ELB runs its healthcheck repeatedly on new registered vms and doesn't put them into service
 * until they pass the healthcheck a certain number of times.  However it has the advantages of leaving public DNS
 * intact and keeping the ELB at its current scale capacity.
 * <p/>
 * To avoid a downtime, we register new first and wait for it to complete before starting to deregister old second.
 * The old app is already frozen.  Assuming the app's internal logic and user experience will be ok with having both
 * apps running during the healthcheck timeframe.
 */
@Lazy
@Component
public class FixedElbzFlipEc2zTask extends TwoEnvTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FixedElbzFlipEc2zTask.class);

  @Autowired
  private Ec2zClientFactory ec2ClientFactory;

  @Autowired
  private ElbzClientFactory elbzClientFactory;

  @Autowired
  @Qualifier("fixedELBFlipEC2Task")
  private WaiterParameters waiterParameters;

  @Autowired
  private ThreadSleeper threadSleeper;

  private Ec2zClient ec2Client;
  private ElbzClient elbzClient;

  private String fixedLbName;

  public Task assign(int position, String liveEnvName, String stageEnvName, String fixedLbName)
  {
    super.assign(position, liveEnvName, stageEnvName);
    this.fixedLbName = fixedLbName;
    return this;
  }

  /**
   * Flips the switch on the Amazon ELB, keeps the ELB fixed in place and swaps the EC2 instances between oldLiveEnv
   * and newLiveEnv.  To avoid a downtime, we register new first and wait for it to complete before starting to
   * deregister old second.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    ec2Client = ec2ClientFactory.create();
    elbzClient = elbzClientFactory.create();
    String oldLiveEc2InstanceId = findLiveEc2InstanceId();
    String newLiveEc2InstanceId = findStageEc2InstanceId();
    registerNewLiveEC2(newLiveEc2InstanceId, noop);
    deregisterOldLiveEC2(oldLiveEc2InstanceId, noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  private String findLiveEc2InstanceId()
  {
    return findEc2InstanceId(liveApplicationVm.getIpAddress());
  }

  private String findStageEc2InstanceId()
  {
    return findEc2InstanceId(stageApplicationVm.getIpAddress());
  }

  private String findEc2InstanceId(String ipAddress)
  {
    Instance ec2Instance = ec2Client.describeInstanceByPrivateIPAddress(ipAddress);
    return ec2Instance.getInstanceId();
  }

  /**
   * Registers the newLive EC2 instance with the ELB.
   */
  private void registerNewLiveEC2(String newLiveEc2InstanceId, boolean noop)
  {
    LOGGER.info(context(liveEnv) + "Register new live EC2 instance with fixed ELB" + noopRemark(noop));
    if (!noop)
    {
      elbzClient.registerInstance(fixedLbName, newLiveEc2InstanceId);
      waitTilEC2InstanceIsInService(newLiveEc2InstanceId);
    }
  }

  /**
   * Uses progressChecker to checks instanceState until it sees ELB report that the EC2 instance is in service.
   * <p/>
   * Progress checker uses aws describe-instances-health, which is more efficient (smaller response payload) than
   * calling describe-load-balancers.
   */
  void waitTilEC2InstanceIsInService(String newLiveEc2InstanceId)
  {
    LOGGER.info(context(liveEnv) + "Waiting for new live EC2 instance to be declared in service");
    ElbzInstanceHealthProgressChecker progressChecker = new ElbzInstanceHealthProgressChecker(fixedLbName,
        newLiveEc2InstanceId, context(stageEnv), elbzClient);
    Waiter<InstanceState> waiter = new Waiter(waiterParameters, threadSleeper, progressChecker);
    InstanceState instanceState = waiter.waitTilDone();
    if (instanceState == null)
    {
      throw new RuntimeException(context(stageEnv) + "ELB says new live EC2 instance was not declared in service");
    }
  }

  /**
   * Deregisters the oldLive EC2 instance from the ELB.
   */
  private void deregisterOldLiveEC2(String oldLiveEc2InstanceId, boolean noop)
  {
    LOGGER.info(context(liveEnv) + "Deregister old live EC2 instance from fixed ELB" + noopRemark(noop));
    if (!noop)
    {
      elbzClient.deregisterInstance(fixedLbName, oldLiveEc2InstanceId);
      waitTilEC2InstanceIsDeregistered(oldLiveEc2InstanceId);
    }
  }

  /**
   * Uses progressChecker to check load balancer's list of instances until the old instance is no longer shown.
   * <p/>
   * Progress checker uses aws describe-load-balancers since aws documentation says describe-instances-health does not
   * apply to the deregistration case.
   */
  void waitTilEC2InstanceIsDeregistered(String oldLiveEc2InstanceId)
  {
    LOGGER.info(context(liveEnv) + "Waiting for old live EC2 instance to be removed from service");
    ElbzInstanceGoneProgressChecker progressChecker = new ElbzInstanceGoneProgressChecker(fixedLbName,
        oldLiveEc2InstanceId, context(liveEnv), elbzClient);
    Waiter<Boolean> waiter = new Waiter(waiterParameters, threadSleeper, progressChecker);
    Boolean gone = waiter.waitTilDone();
    if (gone == null || !gone)
    {
      throw new RuntimeException(context(liveEnv) + "ELB says old live EC2 instance was not removed from service");
    }
  }
}
