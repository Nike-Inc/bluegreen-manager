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
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests error-handling capabilities when mock aws returns unexpected results.
 * <p/>
 * Or for the solely declarative methods, simply tests that the aws client is invoked and returns a result (not much
 * of a test, really).
 */
public class RdszClientTest
{
  private static final String INSTANCE_NAME = "my-rds-instance";
  private static final String ANOTHER_INSTANCE_NAME = "another-rds-instance";
  private static final String SNAPSHOT_ID = "the-snapshot-12354";
  private static final String ANOTHER_SNAPSHOT_ID = "another-snapshot-67893";
  private static final String PARAM_GROUP = "default-mysql";
  private static final String SECURITY_GROUP = "rds-mysql";
  private static final String SUBNET_GROUP = "vpcsubnet";

  private AmazonRDSClient mockRdsClient = mock(AmazonRDSClient.class);
  private RdszClient rdszClient = new RdszClient(mockRdsClient);

  /**
   * Fail case: describe request gets result with empty list of instances.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeInstance_CantFind()
  {
    setupMock(makeDescribeDBInstancesResult(null));
    rdszClient.describeInstance(INSTANCE_NAME);
  }

  /**
   * Warn case: describe request gets result with too many instances.  Returns first one.
   */
  @Test
  public void testDescribeInstance_FoundTooMany()
  {
    setupMock(makeDescribeDBInstancesResult(INSTANCE_NAME, ANOTHER_INSTANCE_NAME));

    DBInstance dbInstance = rdszClient.describeInstance(INSTANCE_NAME);

    assertEquals(INSTANCE_NAME, dbInstance.getDBInstanceIdentifier());
  }

  /**
   * Pass case: describe request gets result with exactly one instance.
   */
  @Test
  public void testDescribeInstance_Pass()
  {
    setupMock(makeDescribeDBInstancesResult(INSTANCE_NAME));

    DBInstance dbInstance = rdszClient.describeInstance(INSTANCE_NAME);

    assertEquals(INSTANCE_NAME, dbInstance.getDBInstanceIdentifier());
  }

  /**
   * Sets up the mock rds client to return a fakeResult for the describe-db-instances call.
   */
  private void setupMock(DescribeDBInstancesResult fakeResult)
  {
    when(mockRdsClient.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(fakeResult);
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
   * Fail case: describe request gets result with empty list of snapshots.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeSnapshot_CantFind()
  {
    setupMock(makeDescribeDBSnapshotsResult(null));
    rdszClient.describeSnapshot(SNAPSHOT_ID);
  }

  /**
   * Warn case: describe request gets result with too many snapshots.  Returns first one.
   */
  @Test
  public void testDescribeSnapshot_FoundTooMany()
  {
    setupMock(makeDescribeDBSnapshotsResult(SNAPSHOT_ID, ANOTHER_SNAPSHOT_ID));

    DBSnapshot dbSnapshot = rdszClient.describeSnapshot(SNAPSHOT_ID);

    assertEquals(SNAPSHOT_ID, dbSnapshot.getDBSnapshotIdentifier());
  }

  /**
   * Pass case: describe request gets result with exactly one snapshot.
   */
  @Test
  public void testDescribeSnapshot_Pass()
  {
    setupMock(makeDescribeDBSnapshotsResult(SNAPSHOT_ID));

    DBSnapshot dbSnapshot = rdszClient.describeSnapshot(SNAPSHOT_ID);

    assertEquals(SNAPSHOT_ID, dbSnapshot.getDBSnapshotIdentifier());
  }

  /**
   * Sets up the mock rds client to return a fakeResult for the describe-db-snapshots call.
   */
  private void setupMock(DescribeDBSnapshotsResult fakeResult)
  {
    when(mockRdsClient.describeDBSnapshots(any(DescribeDBSnapshotsRequest.class))).thenReturn(fakeResult);
  }

  /**
   * Test helper - makes describe result with a named snapshot.
   */
  private DescribeDBSnapshotsResult makeDescribeDBSnapshotsResult(String... snapshotNames)
  {
    DescribeDBSnapshotsResult result = new DescribeDBSnapshotsResult();
    List<DBSnapshot> dbSnapshots = new ArrayList<DBSnapshot>();
    if (ArrayUtils.isNotEmpty(snapshotNames))
    {
      for (String snapshotName : snapshotNames)
      {
        DBSnapshot dbSnapshot = new DBSnapshot();
        dbSnapshot.setDBSnapshotIdentifier(snapshotName);
        dbSnapshots.add(dbSnapshot);
      }
    }
    result.setDBSnapshots(dbSnapshots);
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

    assertEquals(mockSnapshot, rdszClient.createSnapshot(SNAPSHOT_ID, INSTANCE_NAME));
  }

  /**
   * Tests that copyParameterGroup returns its rds result object.
   */
  @Test
  public void testCopyParameterGroup()
  {
    DBParameterGroup mockParamGroup = mock(DBParameterGroup.class);
    when(mockRdsClient.copyDBParameterGroup(any(CopyDBParameterGroupRequest.class))).thenReturn(mockParamGroup);

    assertEquals(mockParamGroup, rdszClient.copyParameterGroup("paramGroup1", "paramGroup2"));
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

    assertEquals(mockInstance, rdszClient.restoreInstanceFromSnapshot(INSTANCE_NAME, SNAPSHOT_ID, SUBNET_GROUP));
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

    assertEquals(mockInstance, rdszClient.modifyInstanceWithSecgrpParamgrp(INSTANCE_NAME, securityGroups, PARAM_GROUP));
  }
}