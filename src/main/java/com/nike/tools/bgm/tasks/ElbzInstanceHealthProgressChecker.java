package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.nike.tools.bgm.client.aws.ElbzClient;
import com.nike.tools.bgm.client.aws.ElbzInstanceState;
import com.nike.tools.bgm.utils.ProgressChecker;

/**
 * Knows how to check progress of an EC2 instance registering with an ELB and heading towards the 'InService' state.
 */
public class ElbzInstanceHealthProgressChecker implements ProgressChecker<InstanceState>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ElbzInstanceHealthProgressChecker.class);

  private String elbName;
  private String ec2InstanceId;
  private String logContext;
  private ElbzClient elbzClient;
  private boolean done;
  private InstanceState result;

  public ElbzInstanceHealthProgressChecker(String elbName,
                                           String ec2InstanceId,
                                           String logContext,
                                           ElbzClient elbzClient)
  {
    this.elbName = elbName;
    this.ec2InstanceId = ec2InstanceId;
    this.logContext = logContext;
    this.elbzClient = elbzClient;
  }

  @Override
  public String getDescription()
  {
    return "ELB Instance Health for elb '" + elbName + "', ec2 instance '" + ec2InstanceId + "'";
  }

  /**
   * Initial check calls elbClient to describe instance health, same as the followup checks, because the initial
   * registration call does not return any health info.
   */
  @Override
  public void initialCheck()
  {
    InstanceState instanceState = elbzClient.describeInstanceHealth(elbName, ec2InstanceId);
    checkInstanceState(instanceState);
    LOGGER.debug(logContext + "Initial ELB instance health: " + instanceState.getState());
  }

  @Override
  public void followupCheck(int waitNum)
  {
    InstanceState instanceState = elbzClient.describeInstanceHealth(elbName, ec2InstanceId);
    checkInstanceState(instanceState);
    LOGGER.debug(logContext + "ELB instance health after wait#" + waitNum + ": " + instanceState.getState());
  }

  /**
   * Sanity checks the instance state, and checks for done-ness.
   */
  private void checkInstanceState(InstanceState instanceState)
  {
    if (!StringUtils.equals(ec2InstanceId, instanceState.getInstanceId()))
    {
      throw new IllegalStateException(logContext + "We requested health of ec2 instance id '" + ec2InstanceId
          + "' but ELB replied with id '" + instanceState.getInstanceId() + "'");
    }
    if (ElbzInstanceState.IN_SERVICE.equalsString(instanceState.getState()))
    {
      LOGGER.info("ELB '" + elbName + "' says ec2 instance id '" + ec2InstanceId + "' is now in service");
      done = true;
      result = instanceState;
    }
  }

  @Override
  public boolean isDone()
  {
    return done;
  }

  @Override
  public InstanceState getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns null.
   */
  @Override
  public InstanceState timeout()
  {
    LOGGER.error("ELB Instance Health failed to reach state '" + ElbzInstanceState.IN_SERVICE + "' prior to timeout");
    return null;
  }
}
