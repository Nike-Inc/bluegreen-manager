package com.nike.tools.bgm.client.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CopyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DBSnapshot;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RDSCopier is mostly declarative, so these tests are a bit thin.
 */
public class RDSCopierTest
{
  private static final String INSTANCE_NAME = "my-rds-instance";
  private static final String ANOTHER_INSTANCE_NAME = "another-rds-instance";
  private static final String SNAPSHOT_ID = "the-snapshot-12354";
  private static final String PARAM_GROUP = "default-mysql";
  private static final String SECURITY_GROUP = "rds-mysql";

  private AmazonRDSClient mockRdsClient = mock(AmazonRDSClient.class);
  private RDSCopier rdsCopier = new RDSCopier(mockRdsClient);

  /**
   * Fail case: describe request gets result with empty list of instances.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeInstance_CantFind()
  {
    DescribeDBInstancesResult fakeResult = makeDescribeDBInstancesResult(null);
    when(mockRdsClient.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(fakeResult);

    rdsCopier.describeInstance(INSTANCE_NAME);
  }

  /**
   * Warn case: describe request gets result with too many instances.  Returns first one.
   */
  @Test
  public void testDescribeInstance_FoundTooMany()
  {
    DescribeDBInstancesResult fakeResult = makeDescribeDBInstancesResult(INSTANCE_NAME, ANOTHER_INSTANCE_NAME);
    when(mockRdsClient.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(fakeResult);

    DBInstance dbInstance = rdsCopier.describeInstance(INSTANCE_NAME);

    assertEquals(INSTANCE_NAME, dbInstance.getDBInstanceIdentifier());
  }

  /**
   * Pass case: describe request gets result with exactly one instance.
   */
  @Test
  public void testDescribeInstance_Pass()
  {
    DescribeDBInstancesResult fakeResult = makeDescribeDBInstancesResult(INSTANCE_NAME);
    when(mockRdsClient.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(fakeResult);

    DBInstance dbInstance = rdsCopier.describeInstance(INSTANCE_NAME);

    assertEquals(INSTANCE_NAME, dbInstance.getDBInstanceIdentifier());
  }

  /**
   * Test helper - makes describe result with a named instance.
   */
  private DescribeDBInstancesResult makeDescribeDBInstancesResult(String... instanceNames)
  {
    DescribeDBInstancesResult result = new DescribeDBInstancesResult();
    List<DBInstance> dbInstances = new ArrayList<DBInstance>();
    if (ArrayUtils.isNotEmpty(instanceNames))
    {
      for (String instanceName : instanceNames)
      {
        DBInstance dbInstance = new DBInstance();
        dbInstance.setDBInstanceIdentifier(instanceName);
        dbInstances.add(dbInstance);
      }
    }
    result.setDBInstances(dbInstances);
    return result;
  }

  /**
   * Tests that createSnapshot returns its rds result object.
   */
  @Test
  public void testCreateSnapshot()
  {
    DBSnapshot mockSnapshot = mock(DBSnapshot.class);
    when(mockRdsClient.createDBSnapshot(any(CreateDBSnapshotRequest.class))).thenReturn(mockSnapshot);

    assertEquals(mockSnapshot, rdsCopier.createSnapshot(SNAPSHOT_ID, INSTANCE_NAME));
  }

  /**
   * Tests that copyParameterGroup returns its rds result object.
   */
  @Test
  public void testCopyParameterGroup()
  {
    DBParameterGroup mockParamGroup = mock(DBParameterGroup.class);
    when(mockRdsClient.copyDBParameterGroup(any(CopyDBParameterGroupRequest.class))).thenReturn(mockParamGroup);

    assertEquals(mockParamGroup, rdsCopier.copyParameterGroup("paramGroup1", "paramGroup2"));
  }

  /**
   * Tests that restoreInstanceFromSnapshot returns its rds result object.
   */
  @Test
  public void testRestoreInstanceFromSnapshot()
  {
    DBInstance mockInstance = mock(DBInstance.class);
    when(mockRdsClient.restoreDBInstanceFromDBSnapshot(any(RestoreDBInstanceFromDBSnapshotRequest.class)))
        .thenReturn(mockInstance);

    assertEquals(mockInstance, rdsCopier.restoreInstanceFromSnapshot(INSTANCE_NAME, SNAPSHOT_ID));
  }

  /**
   * Tests that modifyInstanceWithSecgrpParamgrp returns its rds result object.
   */
  @Test
  public void testModifyInstanceWithSecgrpParamgrp()
  {
    DBInstance mockInstance = mock(DBInstance.class);
    when(mockRdsClient.modifyDBInstance(any(ModifyDBInstanceRequest.class))).thenReturn(mockInstance);
    Collection<String> securityGroups = new ArrayList<String>();
    securityGroups.add(SECURITY_GROUP);

    assertEquals(mockInstance, rdsCopier.modifyInstanceWithSecgrpParamgrp(INSTANCE_NAME, securityGroups, PARAM_GROUP));
  }
}