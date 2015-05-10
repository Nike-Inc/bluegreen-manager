package com.nike.tools.bgm.model.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.nike.tools.bgm.utils.HashUtil;

/**
 * An environment is defined by a list of logical databases.
 */
@Entity
@Table(name = Environment.TABLE_NAME)
public class Environment
{
  public static final String TABLE_NAME = "ENVIRONMENT";
  public static final String COLUMN_ID = "ENV_ID";
  public static final String COLUMN_ENV_NAME = "ENV_NAME";
  public static final int LENGTH_ENV_NAME = 32;

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long envId;

  @Column(name = COLUMN_ENV_NAME, nullable = false, unique = true, length = LENGTH_ENV_NAME)
  private String envName;

  @OneToMany(mappedBy = LogicalDatabase.FIELD_ENVIRONMENT, cascade = CascadeType.ALL)
  private List<LogicalDatabase> logicalDatabases;

  @OneToMany(mappedBy = ApplicationVm.FIELD_ENVIRONMENT, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ApplicationVm> applicationVms;

  public long getEnvId()
  {
    return envId;
  }

  public void setEnvId(long envId)
  {
    this.envId = envId;
  }

  public String getEnvName()
  {
    return envName;
  }

  public void setEnvName(String envName)
  {
    this.envName = envName;
  }

  public List<LogicalDatabase> getLogicalDatabases()
  {
    return logicalDatabases;
  }

  public void setLogicalDatabases(List<LogicalDatabase> logicalDatabases)
  {
    this.logicalDatabases = logicalDatabases;
  }

  public List<ApplicationVm> getApplicationVms()
  {
    return applicationVms;
  }

  public void setApplicationVms(List<ApplicationVm> applicationVms)
  {
    if (this.applicationVms == null)
    {
      this.applicationVms = applicationVms;
    }
    else
    {
      // Avoid unintended orphanRemoval
      this.applicationVms.clear();
      if (applicationVms != null)
      {
        this.applicationVms.addAll(applicationVms);
      }
    }
  }

  public void addLogicalDatabase(LogicalDatabase logicalDatabase)
  {
    if (logicalDatabases == null)
    {
      logicalDatabases = new ArrayList<LogicalDatabase>();
    }
    logicalDatabases.add(logicalDatabase);
  }

  public void addApplicationVm(ApplicationVm applicationVm)
  {
    if (applicationVms == null)
    {
      applicationVms = new ArrayList<ApplicationVm>();
    }
    applicationVms.add(applicationVm);
  }

  public void removeApplicationVm(ApplicationVm applicationVm)
  {
    applicationVms.remove(applicationVm);
  }

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof Environment)
    {
      Environment other = (Environment) obj;
      return envId == other.envId;
    }
    return false;
  }

  /**
   * Hashcode based solely on database identity.
   */
  @Override
  public int hashCode()
  {
    return HashUtil.hashId(envId);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Environment[");
    sb.append("envId: ");
    sb.append(envId);
    sb.append(", envName: ");
    sb.append(envName);
    sb.append(", logicalDatabases: ");
    sb.append(logicalDatabases == null ? "null" : logicalDatabases.toString());
    sb.append(", applicationVms: ");
    sb.append(applicationVms == null ? "null" : applicationVms.toString());
    sb.append("]");
    return sb.toString();
  }
}
