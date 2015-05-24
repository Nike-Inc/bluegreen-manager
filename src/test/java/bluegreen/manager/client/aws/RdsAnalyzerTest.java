package bluegreen.manager.client.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroupStatus;
import com.amazonaws.services.rds.model.VpcSecurityGroupMembership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RdsAnalyzerTest
{
  private static final String INSTANCE_NAME = "my-rds-instance";
  private static final String PARAM_GROUP_PREFIX = "mysql-params:";
  private static final String PARAM_GROUP_DEFAULT = "default-mysql";
  private static final String PARAM_GROUP_INSTANCE_SPECIFIC = PARAM_GROUP_PREFIX + INSTANCE_NAME;
  private static final String SECURITY_GROUP_MYSQL = "rds-mysql";

  private RdsAnalyzer rdsAnalyzer = new RdsAnalyzer();

  /**
   * Tests the self-named case.
   */
  @Test
  public void testFindSelfNamedOrDefaultParamGroupName_SelfNamed()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups(PARAM_GROUP_DEFAULT, PARAM_GROUP_INSTANCE_SPECIFIC);
    assertEquals(PARAM_GROUP_INSTANCE_SPECIFIC, rdsAnalyzer.findSelfNamedOrDefaultParamGroupName(dbInstance));
  }

  /**
   * Tests the default case where a self-named paramgroup can't be found.  Allow default.
   */
  @Test
  public void testFindSelfNamedOrDefaultParamGroupName_Default()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups(PARAM_GROUP_DEFAULT);
    assertEquals(PARAM_GROUP_DEFAULT, rdsAnalyzer.findSelfNamedOrDefaultParamGroupName(dbInstance));
  }

  /**
   * Tests the case with no paramgroups.
   */
  @Test
  public void testFindSelfNamedOrDefaultParamGroupName_NoParamGroups()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups();
    assertNull(rdsAnalyzer.findSelfNamedOrDefaultParamGroupName(dbInstance));
  }

  /**
   * Tests the self-named case.
   */
  @Test
  public void testFindSelfNamedParamGroupName_SelfNamed()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups(PARAM_GROUP_DEFAULT, PARAM_GROUP_INSTANCE_SPECIFIC);
    assertEquals(PARAM_GROUP_INSTANCE_SPECIFIC, rdsAnalyzer.findSelfNamedParamGroupName(dbInstance));
  }

  /**
   * Tests the default case where a self-named paramgroup can't be found.  Default is not a match.
   */
  @Test
  public void testFindSelfNamedParamGroupName_Default()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups(PARAM_GROUP_DEFAULT);
    assertNull(rdsAnalyzer.findSelfNamedParamGroupName(dbInstance));
  }

  /**
   * Tests the case with no paramgroups.
   */
  @Test
  public void testFindSelfNamedParamGroupName_NoParamGroups()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups();
    assertNull(rdsAnalyzer.findSelfNamedParamGroupName(dbInstance));
  }

  private DBInstance makeDBInstanceWithParamGroups(String... paramGroupNames)
  {
    return makeDBInstanceWithParamGroups(RdsParameterApplyStatus.IN_SYNC, paramGroupNames);
  }

  /**
   * Test helper - makes a DBInstance having the specified paramgroup names.
   */
  private DBInstance makeDBInstanceWithParamGroups(RdsParameterApplyStatus parameterApplyStatus,
                                                   String... paramGroupNames)
  {
    DBInstance dbInstance = new DBInstance();
    dbInstance.setDBInstanceIdentifier(INSTANCE_NAME);
    if (ArrayUtils.isNotEmpty(paramGroupNames))
    {
      Collection<DBParameterGroupStatus> paramGroups = new ArrayList<DBParameterGroupStatus>();
      for (String paramGroupName : paramGroupNames)
      {
        paramGroups.add(makeDBParameterGroupStatus(paramGroupName, parameterApplyStatus));
      }
      dbInstance.setDBParameterGroups(paramGroups);
    }
    return dbInstance;
  }

  private DBParameterGroupStatus makeDBParameterGroupStatus(String paramGroupName,
                                                            RdsParameterApplyStatus parameterApplyStatus)
  {
    DBParameterGroupStatus paramGroup = new DBParameterGroupStatus();
    paramGroup.setDBParameterGroupName(paramGroupName);
    paramGroup.setParameterApplyStatus(parameterApplyStatus.toString());
    return paramGroup;
  }

  /**
   * Tests the case of one security group to extract.
   */
  @Test
  public void testExtractVpcSecurityGroupIds()
  {
    DBInstance dbInstance = makeDBInstanceWithSecurityGroups(SECURITY_GROUP_MYSQL);
    Collection<String> securityGroupIds = rdsAnalyzer.extractVpcSecurityGroupIds(dbInstance);
    assertEquals(1, securityGroupIds.size());
    assertEquals(SECURITY_GROUP_MYSQL, securityGroupIds.iterator().next());
  }

  /**
   * Test helper - makes a DBInstance having the specified security group names.
   */
  private DBInstance makeDBInstanceWithSecurityGroups(String... securityGroupIds)
  {
    DBInstance dbInstance = new DBInstance();
    dbInstance.setDBInstanceIdentifier(INSTANCE_NAME);
    if (ArrayUtils.isNotEmpty(securityGroupIds))
    {
      List<VpcSecurityGroupMembership> securityGroups = new ArrayList<VpcSecurityGroupMembership>();
      for (String securityGroupId : securityGroupIds)
      {
        securityGroups.add(makeVpcSecurityGroupMembership(securityGroupId));
      }
      dbInstance.setVpcSecurityGroups(securityGroups);
    }
    return dbInstance;
  }

  private VpcSecurityGroupMembership makeVpcSecurityGroupMembership(String securityGroupId)
  {
    VpcSecurityGroupMembership securityGroup = new VpcSecurityGroupMembership();
    securityGroup.setVpcSecurityGroupId(securityGroupId);
    return securityGroup;
  }

  @Test
  public void testFindParameterApplyStatus()
  {
    DBInstance dbInstance = makeDBInstanceWithParamGroups(RdsParameterApplyStatus.APPLYING, PARAM_GROUP_DEFAULT);
    assertEquals(RdsParameterApplyStatus.APPLYING, rdsAnalyzer.findParameterApplyStatus(dbInstance, PARAM_GROUP_DEFAULT));
  }
}