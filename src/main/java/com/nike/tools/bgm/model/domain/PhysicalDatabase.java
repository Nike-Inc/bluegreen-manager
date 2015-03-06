package com.nike.tools.bgm.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import com.nike.tools.bgm.utils.HashUtil;

/**
 * A physical database has database connection parameters, and when in use maps to a logical database in an environment.
 * <p/>
 * A given logical database can have at most one live physical and one "other" physical, but only the logical knows
 * which is which.  The physical does not track its own liveness because you couldn't set a constraint this way to
 * ensure that only one physical is deemed live.
 */
@Entity
@Table(name = PhysicalDatabase.TABLE_NAME)
public class PhysicalDatabase
{
  public static final String TABLE_NAME = "PHYSICAL_DATABASE";
  public static final String COLUMN_ID = "PHYSICAL_ID";
  public static final String COLUMN_FK_LOGICAL_ID = "FK_LOGICAL_ID";
  public static final String COLUMN_DRIVER_CLASS_NAME = "DRIVER_CLASS_NAME";
  public static final String COLUMN_URL = "URL";
  public static final String COLUMN_USERNAME = "USERNAME";
  public static final String COLUMN_PASSWORD = "PASSWORD";
  public static final int LENGTH_DRIVER_CLASS_NAME = 32;
  public static final int LENGTH_URL = 255;
  public static final int LENGTH_USERNAME = 32;
  public static final int LENGTH_PASSWORD = 32;
  public static final String FIELD_LOGICAL_DATABASE = "logicalDatabase";

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long physicalId;

  @OneToOne
  @JoinColumn(name = COLUMN_FK_LOGICAL_ID)
  private LogicalDatabase logicalDatabase; //FIELD_LOGICAL_DATABASE

  /*
  The following is based on connection parameters relevant to a MySQL database.
  TODO - Generalize to support multiple kinds of physical database technologies
   */

  @Column(name = COLUMN_DRIVER_CLASS_NAME, nullable = false, length = LENGTH_DRIVER_CLASS_NAME)
  private String driverClassName;

  @Column(name = COLUMN_URL, nullable = false, length = LENGTH_URL)
  private String url;

  @Column(name = COLUMN_USERNAME, nullable = false, length = LENGTH_USERNAME)
  private String username;

  @Column(name = COLUMN_PASSWORD, nullable = false, length = LENGTH_PASSWORD)
  private String password;

  public long getPhysicalId()
  {
    return physicalId;
  }

  public void setPhysicalId(long physicalId)
  {
    this.physicalId = physicalId;
  }

  public LogicalDatabase getLogicalDatabase()
  {
    return logicalDatabase;
  }

  public void setLogicalDatabase(LogicalDatabase logicalDatabase)
  {
    this.logicalDatabase = logicalDatabase;
  }

  public String getDriverClassName()
  {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName)
  {
    this.driverClassName = driverClassName;
  }

  public String getUrl()
  {
    return url;
  }

  public void setUrl(String url)
  {
    this.url = url;
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  /**
   * Convenience setter that sets the connection parameters but not id or fk.
   */
  public void setConnectionValues(String driverClassName, String url, String username, String password)
  {
    this.driverClassName = driverClassName;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof PhysicalDatabase)
    {
      PhysicalDatabase other = (PhysicalDatabase) obj;
      return physicalId == other.physicalId;
    }
    return false;
  }

  /**
   * Hashcode based solely on database identity.
   */
  @Override
  public int hashCode()
  {
    return HashUtil.hashId(physicalId);
  }

  /**
   * Equality based on non-id, non-fk fields.
   */
  public boolean isEquivalentTo(PhysicalDatabase other)
  {
    return (other != null
        && StringUtils.equals(driverClassName, other.driverClassName)
        && StringUtils.equals(url, other.url)
        && StringUtils.equals(username, other.username)
        && StringUtils.equals(password, other.password));
  }

  @Override
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("PhysicalDatabase[");
    sb.append("physicalId: ");
    sb.append(physicalId);
    sb.append(", driverClassName: ");
    sb.append(driverClassName);
    sb.append(", url: ");
    sb.append(url);
    sb.append(", username: ");
    sb.append(username);
    //Omit password for security.
    sb.append("]");
    return sb.toString();
  }
}
