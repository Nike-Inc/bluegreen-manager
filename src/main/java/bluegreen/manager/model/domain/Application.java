package bluegreen.manager.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import bluegreen.manager.utils.HashUtil;

/**
 * An application runs on a vm and implements a restful interface at a particular url path whereby the app can
 * receive blue/green directives.
 */
@Entity
@Table(name = Application.TABLE_NAME)
public class Application
{
  public static final String TABLE_NAME = "APPLICATION";
  public static final String COLUMN_ID = "APP_ID";
  public static final String COLUMN_FK_APPVM_ID = "FK_APPVM_ID";
  public static final String COLUMN_SCHEME = "APP_SCHEME"; //http, https
  public static final String COLUMN_HOSTNAME = "APP_HOSTNAME";
  public static final String COLUMN_PORT = "APP_PORT";
  public static final String COLUMN_URL_PATH = "APP_URL_PATH";
  public static final int LENGTH_SCHEME = 10;
  public static final int LENGTH_HOSTNAME = 128;
  public static final int LENGTH_URL_PATH = 255;
  public static final String FIELD_APPLICATION_VM = "applicationVm";
  public static final int CONSTRAINT_MIN_PORT = 0;
  public static final int CONSTRAINT_MAX_PORT = 65535;

  @Id
  @GeneratedValue
  @Column(name = COLUMN_ID)
  private long id;

  @ManyToOne
  @JoinColumn(name = COLUMN_FK_APPVM_ID, nullable = false)
  private ApplicationVm applicationVm; //FIELD_APPLICATION_VM

  @Column(name = COLUMN_SCHEME, nullable = false, length = LENGTH_SCHEME)
  private String scheme;

  @Column(name = COLUMN_HOSTNAME, nullable = false, length = LENGTH_HOSTNAME)
  private String hostname;

  @Min(CONSTRAINT_MIN_PORT)
  @Max(CONSTRAINT_MAX_PORT)
  @Column(name = COLUMN_PORT)
  private Integer port;

  @Pattern(regexp = "^/.*")
  @Column(name = COLUMN_URL_PATH, nullable = false, length = LENGTH_URL_PATH)
  private String urlPath;

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public ApplicationVm getApplicationVm()
  {
    return applicationVm;
  }

  public void setApplicationVm(ApplicationVm applicationVm)
  {
    this.applicationVm = applicationVm;
  }

  public String getScheme()
  {
    return scheme;
  }

  public void setScheme(String scheme)
  {
    this.scheme = scheme;
  }

  public String getHostname()
  {
    return hostname;
  }

  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }

  public Integer getPort()
  {
    return port;
  }

  public void setPort(Integer port)
  {
    this.port = port;
  }

  public String getUrlPath()
  {
    return urlPath;
  }

  public void setUrlPath(String urlPath)
  {
    this.urlPath = urlPath;
  }

  /**
   * Equality based solely on database identity.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof Application)
    {
      Application other = (Application) obj;
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
    sb.append("Application[");
    sb.append("id: ");
    sb.append(id);
    sb.append(", scheme: ");
    sb.append(scheme);
    sb.append(", hostname: ");
    sb.append(hostname);
    sb.append(", port: ");
    sb.append(port);
    sb.append(", urlPath: ");
    sb.append(urlPath);
    sb.append("]");
    return sb.toString();
  }

  /**
   * Produces a URI string 'scheme://hostname:port/urlPath'.
   * In other words the hostname not the ip.
   */
  public String makeHostnameUri()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(scheme);
    sb.append("://");
    sb.append(hostname);
    if (port != null)
    {
      sb.append(":");
      sb.append(port);
    }
    sb.append(urlPath);
    return sb.toString();
  }

  /**
   * Produces a URI string 'scheme://hostname:port/alternateUrlPath'.
   * As an alternative to the normal urlPath.
   * <p/>
   * TODO - Hopefully remove this, it only exists because kraken's bluegreen endpoints are a tad inconsistent.
   */
  public String makeAlternateUri(String alternateUrlPath)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(scheme);
    sb.append("://");
    sb.append(hostname);
    if (port != null)
    {
      sb.append(":");
      sb.append(port);
    }
    sb.append(alternateUrlPath);
    return sb.toString();
  }
}
