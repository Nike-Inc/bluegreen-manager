package bluegreen.manager.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.model.DBInstance;

import bluegreen.manager.client.aws.RdsAnalyzer;
import bluegreen.manager.client.aws.RdsClient;
import bluegreen.manager.client.aws.RdsInstanceStatus;
import bluegreen.manager.client.aws.RdsParameterApplyStatus;

/**
 * Knows how to check progress of an RDS instance whose instance-state is progressing through intermediate states
 * toward a final state, and whose paramgroup-apply-state is also progressing.
 */
public class RdsInstanceParamGroupProgressChecker extends RdsInstanceProgressChecker
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsInstanceParamGroupProgressChecker.class);

  private String paramGroupName;
  private RdsAnalyzer rdsAnalyzer;

  public RdsInstanceParamGroupProgressChecker(String instanceId,
                                              String paramGroupName,
                                              String logContext,
                                              RdsClient rdsClient,
                                              RdsAnalyzer rdsAnalyzer,
                                              DBInstance initialInstance,
                                              RdsInstanceStatus expectedInitialState)
  {
    super(instanceId, logContext, rdsClient, initialInstance, expectedInitialState);
    this.paramGroupName = paramGroupName;
    this.rdsAnalyzer = rdsAnalyzer;
  }

  @Override
  protected String describeExpectedInitialState()
  {
    switch (expectedInitialState)
    {
      case MODIFYING:
        return "Modify Instance including ParamGroup";
      default:
        throw new IllegalArgumentException("Cannot check instance paramgroup progress from initial state '"
            + expectedInitialState + "'");
    }
  }

  @Override
  protected void logInitialStatus()
  {
    LOGGER.debug("Initial RDS " + getDescription() + " instance status: " + initialInstance.getDBInstanceStatus()
        + ", paramgroup status: " + rdsAnalyzer.findParameterApplyStatus(initialInstance, paramGroupName));
  }

  @Override
  protected void logFollowupStatus(int waitNum, DBInstance dbInstance)
  {
    LOGGER.debug("RDS " + getDescription() + " status after wait#" + waitNum + ": " + dbInstance.getDBInstanceStatus()
        + ", paramgroup status: " + rdsAnalyzer.findParameterApplyStatus(dbInstance, paramGroupName));
  }

  /**
   * Checks if the instance is in an acceptable intermediate status, and flags done if at final state.
   * <p/>
   * Instance done-ness and paramgroup done-ness are proceeding in parallel.
   */
  @Override
  protected void checkInstanceStatus(DBInstance dbInstance)
  {
    String instanceStatus = dbInstance.getDBInstanceStatus();
    RdsParameterApplyStatus paramGroupStatus = rdsAnalyzer.findParameterApplyStatus(dbInstance, paramGroupName);
    boolean instanceDone = expectedFinalState.equalsString(instanceStatus);
    boolean paramGroupDone = paramGroupStatus == RdsParameterApplyStatus.PENDING_REBOOT;
    if (instanceDone && paramGroupDone)
    {
      LOGGER.info("RDS " + getDescription() + " is done");
      done = true;
      result = dbInstance;
    }
    else
    {
      boolean instanceError = !instanceDone && !isOneOfTheseStates(expectedIntermediateStates, instanceStatus);
      boolean paramGroupError = !paramGroupDone && paramGroupStatus != RdsParameterApplyStatus.APPLYING;
      if (instanceError || paramGroupError)
      {
        LOGGER.error(logContext + getDescription() + ": Unexpected response: instance status '" + instanceStatus
            + "', paramgroup status '" + rdsAnalyzer.findParameterApplyStatus(dbInstance, paramGroupName) + "'");
        done = true;
      }
      //Else just keep waiting
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

}
