package bluegreen.manager.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import bluegreen.manager.utils.HashUtil;

@Entity
@Table(name = Datacenter.TABLE_NAME)
public class Datacenter {

  public static final String TABLE_NAME = "DATACENTER";
  public static final String COLUMN_DATACENTER_ID = "DATACENTER_ID";
  public static final String COLUMN_DATACENTER_NAME = "DATACENTER_NAME";

  @Id
  @GeneratedValue
  @Column(name = COLUMN_DATACENTER_ID)
  private long datacenterId;

  @Column(name = COLUMN_DATACENTER_NAME, nullable = false, unique = true)
  private String datacenterName;

  /**
   * Equality based solely on Datacenter ID
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Datacenter) {
      Datacenter other = (Datacenter) obj;
      return datacenterId == other.datacenterId;
    }
    return false;
  }

  /**
   * Hashcode based solely on Datacenter ID
   */
  @Override
  public int hashCode() {
    return HashUtil.hashId(datacenterId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Datacenter[");
    sb.append("datacenterId: ");
    sb.append(datacenterId);
    sb.append(", datacenterName: ");
    sb.append(datacenterName);
    sb.append("]");
    return sb.toString();
  }
}
