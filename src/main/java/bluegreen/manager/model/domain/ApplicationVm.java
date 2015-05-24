package bluegreen.manager.model.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import bluegreen.manager.utils.HashUtil;

/**
 * A vm on which we will deploy application packages.  It belongs to an environment.
 */
@Entity
@Table(name = ApplicationVm.TABLE_NAME)
public class ApplicationVm
{
  public static final String TABLE_NAME = "APPLICATION_VM";
  public static final String COLUMN_ID = "APPVM_ID";
  public static final String COLUMN_FK_ENV_ID = "FK_ENV_ID";
  public static final String COLUMN_HOSTNAME = "APPVM_HOSTNAME";
  public static final String COLUMN_IP_ADDRESS = "APPVM_IP_ADDRESS";
  public static final int LENGTH_HOSTNAME = 128;
  public static final int LENGTH_IP_ADDRESS = 20;
  public static final String FIELD_ENVIRONMENT = "environment";

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long id;

  @ManyToOne
  @JoinColumn(name = COLUMN_FK_ENV_ID, nullable = false)
  private Environment environment; //FIELD_ENVIRONMENT

  @Column(name = COLUMN_HOSTNAME, nullable = false, length = LENGTH_HOSTNAME)
  private String hostname;

  @Column(name = COLUMN_IP_ADDRESS, nullable = false, length = LENGTH_IP_ADDRESS)
  private String ipAddress;

  @OneToMany(mappedBy = Application.FIELD_APPLICATION_VM, cascade = CascadeType.ALL)
  private List<Application> applications;

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public Environment getEnvironment()
  {
    return environment;
  }

  public void setEnvironment(Environment environment)
  {
    this.environment = environment;
  }

  public String getHostname()
  {
    return hostname;
  }

  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }

  public String getIpAddress()
  {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress)
  {
    this.ipAddress = ipAddress;
  }

  public List<Application> getApplications()
  {
    return applications;
  }

  public void setApplications(List<Application> applications)
  {
    this.applications = applications;
  }

  public void addApplication(Application application)
  {
    if (applications == null)
    {
      applications = new ArrayList<Application>();
    }
    applications.add(application);
  }

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof ApplicationVm)
    {
      ApplicationVm other = (ApplicationVm) obj;
      return id == other.id;
    }
    return false;
  }

  /**
   * Hashcode based solely on database identity.
   */
  @Override
  public int hashCode()
  {
    return HashUtil.hashId(id);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("ApplicationVm[");
    sb.append("id: ");
    sb.append(id);
    sb.append(", hostname: ");
    sb.append(hostname);
    sb.append(", ipAddress: ");
    sb.append(ipAddress);
    sb.append(", applications: ");
    sb.append(applications == null ? "null" : applications.toString());
    sb.append("]");
    return sb.toString();
  }
}
