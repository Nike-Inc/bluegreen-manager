package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.nike.tools.bgm.client.aws.RdsClient;
import com.nike.tools.bgm.client.aws.RdsInstanceStatus;
import com.nike.tools.bgm.utils.ProgressChecker;

/**
 * Knows how to check progress of an RDS instance whose state is progressing through intermediate states
 * toward a final state.
 */
public class RdsInstanceProgressChecker implements ProgressChecker<DBInstance>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsInstanceProgressChecker.class);
  private static final RdsInstanceStatus CREATE_FINAL_STATE = RdsInstanceStatus.AVAILABLE;
  private static final RdsInstanceStatus MODIFY_FINAL_STATE = RdsInstanceStatus.AVAILABLE;
  private static final RdsInstanceStatus DELETE_FINAL_STATE = RdsInstanceStatus.DELETED;
  private static final RdsInstanceStatus REBOOT_FINAL_STATE = RdsInstanceStatus.AVAILABLE;
  private static final RdsInstanceStatus[] CREATE_INTERMEDIATE_STATES = new RdsInstanceStatus[] {
      RdsInstanceStatus.CREATING, RdsInstanceStatus.BACKING_UP, RdsInstanceStatus.MODIFYING
  };
  private static final RdsInstanceStatus[] MODIFY_INTERMEDIATE_STATES = new RdsInstanceStatus[] {
      RdsInstanceStatus.MODIFYING
  };
  private static final RdsInstanceStatus[] DELETE_INTERMEDIATE_STATES = new RdsInstanceStatus[] {
      RdsInstanceStatus.DELETING
  };
  private static final RdsInstanceStatus[] REBOOT_INTERMEDIATE_STATES = new RdsInstanceStatus[] {
      RdsInstanceStatus.REBOOTING
  };

  private String instanceId;
  protected String logContext;
  private RdsClient rdsClient;
  protected DBInstance initialInstance;
  protected RdsInstanceStatus expectedInitialState;
  protected RdsInstanceStatus[] expectedIntermediateStates;
  protected RdsInstanceStatus expectedFinalState;
  protected boolean done;
  protected DBInstance result;

  public RdsInstanceProgressChecker(String instanceId,
                                    String logContext,
                                    RdsClient rdsClient,
                                    DBInstance initialInstance,
                                    RdsInstanceStatus expectedInitialState)
  {
    this.instanceId = instanceId;
    this.logContext = logContext;
    this.rdsClient = rdsClient;
    this.initialInstance = initialInstance;
    this.expectedInitialState = expectedInitialState;
    this.expectedIntermediateStates = getExpectedIntermediateStates();
    this.expectedFinalState = getExpectedFinalState();
  }

  @Override
  public String getDescription()
  {
    return describeExpectedInitialState() + " '" + instanceId + "'";
  }

  protected String describeExpectedInitialState()
  {
    switch (expectedInitialState)
    {
      case CREATING:
        return "Create Instance";
      case MODIFYING:
        return "Modify Instance";
      case DELETING:
        return "Delete Instance";
      case REBOOTING:
        return "Reboot Instance";
      default:
        throw new IllegalArgumentException("Cannot check progress from initial state '" + expectedInitialState + "'");
    }
  }

  private RdsInstanceStatus[] getExpectedIntermediateStates()
  {
    switch (expectedInitialState)
    {
      case CREATING:
        return CREATE_INTERMEDIATE_STATES;
      case MODIFYING:
        return MODIFY_INTERMEDIATE_STATES;
      case DELETING:
        return DELETE_INTERMEDIATE_STATES;
      case REBOOTING:
        return REBOOT_INTERMEDIATE_STATES;
      default:
        throw new IllegalArgumentException("Cannot check progress from initial state '" + expectedInitialState + "'");
    }
  }

  private RdsInstanceStatus getExpectedFinalState()
  {
    switch (expectedInitialState)
    {
      case CREATING:
        return CREATE_FINAL_STATE;
      case MODIFYING:
        return MODIFY_FINAL_STATE;
      case DELETING:
        return DELETE_FINAL_STATE;
      case REBOOTING:
        return REBOOT_FINAL_STATE;
      default:
        throw new IllegalArgumentException("Cannot check progress from initial state '" + expectedInitialState + "'");
    }
  }

  /**
   * Checks initial response instance.
   */
  @Override
  public void initialCheck()
  {
    logInitialStatus();
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
    try
    {
      DBInstance dbInstance = rdsClient.describeInstance(instanceId);
      checkInstanceId(dbInstance);
      logFollowupStatus(waitNum, dbInstance);
      checkInstanceStatus(dbInstance);
    }
    catch (DBInstanceNotFoundException e)
    {
      handleInstanceNotFound(waitNum, e);
    }
  }

  protected void logInitialStatus()
  {
    LOGGER.debug("Initial RDS " + getDescription() + " status: " + initialInstance.getDBInstanceStatus());
  }

  protected void logFollowupStatus(int waitNum, DBInstance dbInstance)
  {
    LOGGER.debug("RDS " + getDescription() + " status after wait#" + waitNum + ": " + dbInstance.getDBInstanceStatus());
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
   * Checks if the instance is in an acceptable intermediate status, and flags done if at final state.
   */
  protected void checkInstanceStatus(DBInstance dbInstance)
  {
    final String status = dbInstance.getDBInstanceStatus();
    if (expectedFinalState.equalsString(status))
    {
      LOGGER.info("RDS " + getDescription() + " is done");
      done = true;
      result = dbInstance;
    }
    else if (isOneOfTheseStates(expectedIntermediateStates, status))
    {
      //Keep waiting.
    }
    else
    {
      LOGGER.error(logContext + getDescription() + ": Unexpected response status '" + status + "'");
      done = true;
    }
  }

  /**
   * True if the status is in the array.
   */
  private boolean isOneOfTheseStates(RdsInstanceStatus[] array, String status)
  {
    if (StringUtils.isNotBlank(status))
    {
      for (RdsInstanceStatus oneStatus : array)
      {
        if (oneStatus.equalsString(status))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Amazon can't find the instance.  This is an allowed final state for a delete operation, otherwise is a bad error.
   */
  private void handleInstanceNotFound(int waitNum, DBInstanceNotFoundException e)
  {
    LOGGER.debug("RDS " + getDescription() + " status after wait#" + waitNum + ": " + e.getClass().getSimpleName()
        + ": " + e.getMessage());
    if (expectedFinalState.equals(DELETE_FINAL_STATE))
    {
      LOGGER.info("RDS " + getDescription() + " is done");
      result = new DBInstance(); //Just a stub, since result==null would be considered a progress error.
    }
    else
    {
      LOGGER.error(logContext + getDescription() + ": not found", e);
    }
    done = true;
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
    LOGGER.error(getDescription() + " failed to reach " + expectedFinalState + " state prior to timeout");
    return null;
  }
}
