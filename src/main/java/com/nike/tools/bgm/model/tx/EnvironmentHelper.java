package com.nike.tools.bgm.model.tx;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;

/**
 * Environment-related helper routines that have nothing to do with persistence, network calls or other side effects.
 */
@Component
public class EnvironmentHelper
{
  /**
   * Makes a comma-separated list of database names.
   * Uses logical name (with physical instname and url in parentheses if applicable).
   */
  public String listOfNames(List<LogicalDatabase> logicalDatabases)
  {
    StringBuilder sb = new StringBuilder();
    if (logicalDatabases != null)
    {
      for (LogicalDatabase logicalDatabase : logicalDatabases)
      {
        if (sb.length() > 0)
        {
          sb.append(", ");
        }
        sb.append(logicalDatabase.getLogicalName());
        PhysicalDatabase physicalDatabase = logicalDatabase.getPhysicalDatabase();
        if (physicalDatabase != null)
        {
          sb.append(" (");
          sb.append(physicalDatabase.getInstanceName());
          sb.append(" - ");
          sb.append(physicalDatabase.getUrl());
          sb.append(")");
        }
      }
    }
    return sb.toString();
  }

}
