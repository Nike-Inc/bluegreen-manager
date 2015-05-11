package com.nike.tools.bgm.client.aws;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CopyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DeleteDBParameterGroupRequest;
import com.amazonaws.services.rds.model.DeleteDBSnapshotRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;

/**
 * CRUD operations on Amazon RDS instances.
 * <p/>
 * All operations communicate with Amazon and use a StopWatch.
 */
public class RdsClient
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsClient.class);
  private static final String PARAM_GROUP_DESCRIPTION = "Nonshared so we can toggle read_only param.";

  /**
   * Synchronous client, requests will block til done.
   */
  private AmazonRDSClient awsRdsClient;

  public RdsClient(AmazonRDSClient awsRdsClient)
  {
    this.awsRdsClient = awsRdsClient;
  }

  /**
   * Gets a description of the requested RDS instance.  Throws if not found.
   */
  public DBInstance describeInstance(String instanceName)
  {
    LOGGER.debug("describeDBInstances(instanceName: " + instanceName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
      request.setDBInstanceIdentifier(instanceName);
      DescribeDBInstancesResult result = awsRdsClient.describeDBInstances(request);
      if (result == null || CollectionUtils.isEmpty(result.getDBInstances()))
      {
        throw new RuntimeException("RDS cannot find instance '" + instanceName + "'");
      }
      else if (result.getDBInstances().size() > 1)
      {
        LOGGER.warn("Expected 1 instance named '" + instanceName + "', found " + result.getDBInstances().size());
      }
      return result.getDBInstances().get(0);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("describeDBInstances time elapsed: " + stopWatch);
    }
  }

  /**
   * Gets a description of the requested RDS snapshot.  Throws if not found.
   */
  public DBSnapshot describeSnapshot(String snapshotId)
  {
    LOGGER.debug("describeDBSnapshots(snapshotId: " + snapshotId + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DescribeDBSnapshotsRequest request = new DescribeDBSnapshotsRequest();
      request.setDBSnapshotIdentifier(snapshotId);
      DescribeDBSnapshotsResult result = awsRdsClient.describeDBSnapshots(request);
      if (result == null || CollectionUtils.isEmpty(result.getDBSnapshots()))
      {
        throw new RuntimeException("RDS cannot find snapshot '" + snapshotId + "'");
      }
      else if (result.getDBSnapshots().size() > 1)
      {
        LOGGER.warn("Expected 1 snapshot named '" + snapshotId + "', found " + result.getDBSnapshots().size());
      }
      return result.getDBSnapshots().get(0);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("describeDBSnapshots time elapsed: " + stopWatch);
    }
  }

  /**
   * Creates an RDS instance snapshot using the specified snapshot id.
   * <p/>
   * Caller must wait for status=available afterwards.
   */
  public DBSnapshot createSnapshot(String snapshotId, String instanceName)
  {
    LOGGER.debug("createDBSnapshot(snapshotId: " + snapshotId + ", instanceName: " + instanceName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      CreateDBSnapshotRequest request = new CreateDBSnapshotRequest(snapshotId, instanceName);
      return awsRdsClient.createDBSnapshot(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("createDBSnapshot time elapsed: " + stopWatch);
    }
  }

  /**
   * Copies an RDS parameter group.
   */
  public DBParameterGroup copyParameterGroup(String sourceParamGroupName, String destParamGroupName)
  {
    LOGGER.debug("copyDBParameterGroup(sourceParamGroupName: " + sourceParamGroupName + ", destParamGroupName: "
        + destParamGroupName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      CopyDBParameterGroupRequest request = new CopyDBParameterGroupRequest();
      request.setSourceDBParameterGroupIdentifier(sourceParamGroupName);
      request.setTargetDBParameterGroupIdentifier(destParamGroupName);
      request.setTargetDBParameterGroupDescription(PARAM_GROUP_DESCRIPTION);
      return awsRdsClient.copyDBParameterGroup(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("copyDBParameterGroup time elapsed: " + stopWatch);
    }
  }

  /**
   * Restores a snapshot to a brand new instance.
   * <p/>
   * New instance gets the default security group, otherwise should be same as snapshot.
   * <p/>
   * Caller must wait for status=available afterwards.
   */
  public DBInstance restoreInstanceFromSnapshot(String instanceName, String snapshotId, String subnetGroupName)
  {
    LOGGER.debug("restoreDBInstanceFromDBSnapshot(instanceName: " + instanceName + ", snapshotId: " + snapshotId
        + ", subnetGroupName: " + subnetGroupName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDBInstanceFromDBSnapshotRequest(
          instanceName, snapshotId);
      request.setDBSubnetGroupName(subnetGroupName);
      return awsRdsClient.restoreDBInstanceFromDBSnapshot(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("restoreDBInstanceFromDBSnapshot time elapsed: " + stopWatch);
    }
  }

  /**
   * Modifies the instance by applying new security groups and new parameter group.
   * <p/>
   * Caller must wait for status=available afterwards.
   */
  public DBInstance modifyInstanceWithSecgrpParamgrp(String instanceName,
                                                     Collection<String> vpcSecurityGroupIds,
                                                     String paramGroupName)
  {
    LOGGER.debug("modifyDBInstance(instanceName: " + instanceName + ", vpcSecurityGroupIds: ("
        + StringUtils.join(vpcSecurityGroupIds, ", ") + "), paramGroupName: " + paramGroupName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      ModifyDBInstanceRequest request = new ModifyDBInstanceRequest(instanceName);
      request.setVpcSecurityGroupIds(vpcSecurityGroupIds);
      request.setDBParameterGroupName(paramGroupName);
      return awsRdsClient.modifyDBInstance(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("modifyDBInstance time elapsed: " + stopWatch);
    }
  }

  /**
   * Requests deletion of the instance, without creating a final snapshot or deleting any other related
   * snapshots.
   * <p/>
   * Caller must wait for status=deleted afterwards.
   */
  public DBInstance deleteInstance(String instanceName)
  {
    LOGGER.debug("deleteDBInstance(instanceName: " + instanceName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DeleteDBInstanceRequest request = new DeleteDBInstanceRequest(instanceName);
      request.setSkipFinalSnapshot(true);
      return awsRdsClient.deleteDBInstance(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("deleteDBInstance time elapsed: " + stopWatch);
    }
  }

  /**
   * Deletes the parameter group.  (Assuming it is not in use by any database instance.)
   */
  public void deleteParameterGroup(String paramGroupName)
  {
    LOGGER.debug("deleteDBParameterGroup(paramGroupName: " + paramGroupName + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DeleteDBParameterGroupRequest request = new DeleteDBParameterGroupRequest(paramGroupName);
      awsRdsClient.deleteDBParameterGroup(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("deleteDBParameterGroup time elapsed: " + stopWatch);
    }
  }

  /**
   * Deletes the snapshot.  (Assuming it was in available state.)
   * <p/>
   * Caller must wait for status=deleted afterwards.
   */
  public DBSnapshot deleteSnapshot(String snapshotId)
  {
    LOGGER.debug("deleteDBSnapshot(snapshotId: " + snapshotId + ")");
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      DeleteDBSnapshotRequest request = new DeleteDBSnapshotRequest(snapshotId);
      return awsRdsClient.deleteDBSnapshot(request);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("deleteDBSnapshot time elapsed: " + stopWatch);
    }
  }
}
