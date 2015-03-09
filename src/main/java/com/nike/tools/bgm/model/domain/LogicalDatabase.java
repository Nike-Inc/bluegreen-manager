package com.nike.tools.bgm.model.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.nike.tools.bgm.utils.HashUtil;

/**
 * Apps in an environment will be configured to use a particular logical database (by name).  The bluegreen manager
 * maps it to a physical database with actual connection info.
 * <p/>
 * A logical database has at most one physical database.  Zero if the logical is about to be torn down.
 * <p/>
 * One environment can have many logical databases.  A logical db can only belong to one env.
 */
@Entity
@Table(
    name = LogicalDatabase.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(columnNames = {
        LogicalDatabase.COLUMN_FK_ENV_ID, LogicalDatabase.COLUMN_LOGICAL_NAME
    })
)
public class LogicalDatabase
{
  public static final String TABLE_NAME = "LOGICAL_DATABASE";
  public static final String COLUMN_ID = "LOGICAL_ID";
  public static final String COLUMN_FK_ENV_ID = "FK_ENV_ID";
  public static final String COLUMN_LOGICAL_NAME = "LOGICAL_NAME";
  public static final int LENGTH_LOGICAL_NAME = 32;
  public static final String FIELD_ENVIRONMENT = "environment";

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long logicalId;

  @ManyToOne
  @JoinColumn(name = COLUMN_FK_ENV_ID, nullable = false)
  private Environment environment; //FIELD_ENVIRONMENT

  @Column(name = COLUMN_LOGICAL_NAME, nullable = false, length = LENGTH_LOGICAL_NAME)
  private String logicalName;

  @OneToOne(mappedBy = PhysicalDatabase.FIELD_LOGICAL_DATABASE, cascade = CascadeType.ALL)
  private PhysicalDatabase physicalDatabase;

  public long getLogicalId()
  {
    return logicalId;
  }

  public void setLogicalId(long logicalId)
  {
    this.logicalId = logicalId;
  }

  public Environment getEnvironment()
  {
    return environment;
  }

  public void setEnvironment(Environment environment)
  {
    this.environment = environment;
  }

  public String getLogicalName()
  {
    return logicalName;
  }

  public void setLogicalName(String logicalName)
  {
    this.logicalName = logicalName;
  }

  public PhysicalDatabase getPhysicalDatabase()
  {
    return physicalDatabase;
  }

  public void setPhysicalDatabase(PhysicalDatabase physicalDatabase)
  {
    this.physicalDatabase = physicalDatabase;
  }

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof LogicalDatabase)
    {
      LogicalDatabase other = (LogicalDatabase) obj;
      return logicalId == other.logicalId;
    }
    return false;
  }

  /**
   * Hashcode based solely on database identity.
   */
  @Override
  public int hashCode()
  {
    return HashUtil.hashId(logicalId);
  }

  @Override
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("LogicalDatabase[");
    sb.append("logicalId: ");
    sb.append(logicalId);
    sb.append(", logicalName: ");
    sb.append(logicalName);
    sb.append(", physicalDatabase: ");
    sb.append(physicalDatabase.toString());
    sb.append("]");
    return sb.toString();
  }
}
