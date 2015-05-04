package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBInstance;
import com.nike.tools.bgm.client.aws.InstanceStatus;
import com.nike.tools.bgm.client.aws.RDSClient;
import com.nike.tools.bgm.utils.ProgressChecker;

/**
 * Knows how to check progress of an RDS instance going from 'creating' to 'available'.
 */
public class InstanceProgressChecker implements ProgressChecker<DBInstance>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(InstanceProgressChecker.class);
  private static final InstanceStatus[] CREATE_INTERMEDIATE_STATES = new InstanceStatus[] {
      InstanceStatus.CREATING, InstanceStatus.BACKING_UP, InstanceStatus.MODIFYING
  };
  private static final InstanceStatus[] MODIFY_INTERMEDIATE_STATES = new InstanceStatus[] {
      InstanceStatus.MODIFYING
  };

  private String instanceId;
  private String logContext;
  private RDSClient rdsClient;
  private DBInstance initialInstance;
  private boolean create; //False: modify
  private InstanceStatus[] intermediateStates;
  private boolean done;
  private DBInstance result;

  public InstanceProgressChecker(String instanceId,
                                 String logContext,
                                 RDSClient rdsClient, DBInstance initialInstance, boolean create)
  {
    this.instanceId = instanceId;
    this.logContext = logContext;
    this.rdsClient = rdsClient;
    this.initialInstance = initialInstance;
    this.create = create;
    this.intermediateStates = create ? CREATE_INTERMEDIATE_STATES : MODIFY_INTERMEDIATE_STATES;
  }

  @Override
  public String getDescription()
  {
    return (create ? "Create Instance" : "Modify Instance") + " '" + instanceId + "'";
  }

  /**
   * Checks initial response instance.
   */
  @Override
  public void initialCheck()
  {
    LOGGER.debug("Initial RDS " + getDescription() + " status: " + initialInstance.getDBInstanceStatus());
    checkInstanceId(initialInstance);
    checkInstanceStatus(initialInstance);
  }

  /**
   * Communicates with RDS for updated instance progress and checks the status.
   * Concludes if error or if naturally done.
   */
  @Override
  public void followupCheck(int waitNum)
  {
    DBInstance dbInstance = rdsClient.describeInstance(instanceId);
    checkInstanceId(dbInstance);
    LOGGER.debug("RDS " + getDescription() + " status after wait#" + waitNum + ": " + dbInstance.getDBInstanceStatus());
    checkInstanceStatus(dbInstance);
  }

  /**
   * Asserts that the instance has the expected id.
   */
  private void checkInstanceId(DBInstance dbInstance)
  {
    final String responseInstanceId = dbInstance.getDBInstanceIdentifier();
    if (!StringUtils.equals(instanceId, responseInstanceId))
    {
      throw new IllegalStateException(logContext + "We requested instance id '" + instanceId
          + "' but RDS replied with identifier '" + responseInstanceId + "'");
    }
  }

  /**
   * Checks if the instance is in an acceptable intermediate status, and flags done if status=available.
   */
  private void checkInstanceStatus(DBInstance dbInstance)
  {
    final String status = dbInstance.getDBInstanceStatus();
    final String instanceId = dbInstance.getDBInstanceIdentifier();
    if (InstanceStatus.AVAILABLE.equalsString(status))
    {
      LOGGER.info("RDS " + getDescription() + " '" + instanceId + "' is done");
      done = true;
      result = dbInstance;
    }
    else if (isOneOfTheseStates(intermediateStates, status))
    {
      //Keep waiting.
    }
    else
    {
      LOGGER.error(logContext + getDescription() + " '" + instanceId + "': Unexpected response status '" + status + "'");
      done = true;
    }
  }

  /**
   * True if the status is in the array of intermediateStates.
   */
  private boolean isOneOfTheseStates(InstanceStatus[] intermediateStates, String status)
  {
    if (StringUtils.isNotBlank(status))
    {
      for (InstanceStatus intermediateState : intermediateStates)
      {
        if (intermediateState.equalsString(status))
        {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isDone()
  {
    return done;
  }

  /**
   * True if the instance has become available prior to timeout.
   * False if error.  Null if still in prior states or timeout.
   */
  @Override
  public DBInstance getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns null.
   */
  @Override
  public DBInstance timeout()
  {
    LOGGER.error(getDescription() + " failed to become available prior to timeout");
    return null;
  }
}
