package com.nike.tools.bgm.client.ssh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Defines a target system into which we can ssh.
 * <p/>
 * Currently only support one of these in the whole app! i.e. use of @Value.
 */
@Lazy
@Component
public class SshTarget
{
  @Value("${bluegreen.sshtarget.hostname}")
  private String hostname;

  @Value("${bluegreen.sshtarget.username}")
  private String username;

  @Value("${bluegreen.sshtarget.password}")
  private String password;

  @Value("${bluegreen.sshtarget.connectTimeoutMilliseconds}")
  private long connectTimeoutMilliseconds;

  public SshTarget()
  {
  }

  public SshTarget(String hostname, String username, String password, long connectTimeoutMilliseconds)
  {
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
  }

  public String getHostname()
  {
    return hostname;
  }

  public void setHostname(String hostname)
  {
    this.hostname = hostname;
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

  public long getConnectTimeoutMilliseconds()
  {
    return connectTimeoutMilliseconds;
  }

  public void setConnectTimeoutMilliseconds(long connectTimeoutMilliseconds)
  {
    this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
  }
}
