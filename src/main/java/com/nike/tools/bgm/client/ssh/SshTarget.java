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

  /**
   * Ganymed-specific parameter applied to java.net.Socket SO_TIMEOUT and Socket.connect(addr, timeout).
   * <p/>
   * SO_TIMEOUT is the max time the socket will block on a call to read().
   */
  @Value("${bluegreen.sshtarget.soTimeoutMilliseconds}")
  private long soTimeoutMilliseconds;

  /**
   * Ganymed-specific parameter applied to ssh KEX timeout.
   * <p/>
   * Max time the ssh library will wait for completion of initial ssh handshake.
   */
  @Value("${bluegreen.sshtarget.keyExchangeTimeoutMilliseconds}")
  private long keyExchangeTimeoutMilliseconds;

  public SshTarget()
  {
  }

  public SshTarget(String hostname,
                   String username,
                   String password,
                   long soTimeoutMilliseconds,
                   long keyExchangeTimeoutMilliseconds)
  {
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.soTimeoutMilliseconds = soTimeoutMilliseconds;
    this.keyExchangeTimeoutMilliseconds = keyExchangeTimeoutMilliseconds;
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

  public long getSoTimeoutMilliseconds()
  {
    return soTimeoutMilliseconds;
  }

  public void setSoTimeoutMilliseconds(long soTimeoutMilliseconds)
  {
    this.soTimeoutMilliseconds = soTimeoutMilliseconds;
  }

  public long getKeyExchangeTimeoutMilliseconds()
  {
    return keyExchangeTimeoutMilliseconds;
  }

  public void setKeyExchangeTimeoutMilliseconds(long keyExchangeTimeoutMilliseconds)
  {
    this.keyExchangeTimeoutMilliseconds = keyExchangeTimeoutMilliseconds;
  }
}
