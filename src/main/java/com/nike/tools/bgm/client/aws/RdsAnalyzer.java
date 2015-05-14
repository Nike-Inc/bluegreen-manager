package com.nike.tools.bgm.client.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBParameterGroupStatus;
import com.amazonaws.services.rds.model.VpcSecurityGroupMembership;

/**
 * Analyzes RDS api objects.
 * <p/>
 * Does NOT communicate with Amazon.
 */
@Component
public class RdsAnalyzer
{
  private static final Logger LOGGER = LoggerFactory.getLogger(RdsAnalyzer.class);

  /**
   * Finds the name of the instance's paramgroup whose name embeds the instname.  If not found, then simply returns
   * the first paramgroup name.
   */
  public String findSelfNamedOrDefaultParamGroupName(DBInstance dbInstance)
  {
    String selfNamedGroupName = findSelfNamedParamGroupName(dbInstance);
    if (selfNamedGroupName != null)
    {
      return selfNamedGroupName;
    }
    if (dbInstance != null && CollectionUtils.isNotEmpty(dbInstance.getDBParameterGroups()))
    {
      String defaultName = dbInstance.getDBParameterGroups().get(0).getDBParameterGroupName();
      LOGGER.warn("Could not find paramgroup containing the string '" + dbInstance.getDBInstanceIdentifier()
          + "', falling back to '" + defaultName + "'");
      return defaultName;
    }
    return null;
  }

  /**
   * Finds the name of the instance's paramgroup whose name embeds the instname, or null if not found.
   * We are assuming there is at most one such paramgroup!
   */
  public String findSelfNamedParamGroupName(DBInstance dbInstance)
  {
    if (dbInstance != null && CollectionUtils.isNotEmpty(dbInstance.getDBParameterGroups()))
    {
      String instanceName = dbInstance.getDBInstanceIdentifier();
      for (DBParameterGroupStatus paramGroup : dbInstance.getDBParameterGroups())
      {
        String paramGroupName = paramGroup.getDBParameterGroupName();
        if (StringUtils.contains(paramGroupName, instanceName))
        {
          return paramGroup.getDBParameterGroupName();
        }
      }
    }
    return null;
  }

  /**
   * Extracts the VPC security group ids from the instance.
   */
  public Collection<String> extractVpcSecurityGroupIds(DBInstance dbInstance)
  {
    if (dbInstance != null && CollectionUtils.isNotEmpty(dbInstance.getVpcSecurityGroups()))
    {
      List<String> ids = new ArrayList<String>();
      for (VpcSecurityGroupMembership group : dbInstance.getVpcSecurityGroups())
      {
        ids.add(group.getVpcSecurityGroupId());
      }
      return ids;
    }
    return null;
  }

  /**
   * Finds the parameter-apply-status of the named parameter group inside the dbInstance.
   */
  public RdsParameterApplyStatus findParameterApplyStatus(DBInstance dbInstance, String paramGroupName)
  {
    if (dbInstance != null && dbInstance.getDBParameterGroups() != null && StringUtils.isNotBlank(paramGroupName))
    {
      for (DBParameterGroupStatus paramGroupStatus : dbInstance.getDBParameterGroups())
      {
        if (StringUtils.equals(paramGroupName, paramGroupStatus.getDBParameterGroupName()))
        {
          return RdsParameterApplyStatus.fromString(paramGroupStatus.getParameterApplyStatus());
        }
      }
    }
    return null;
  }
}
