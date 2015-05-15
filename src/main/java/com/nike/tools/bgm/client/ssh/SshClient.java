package com.nike.tools.bgm.client.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.utils.ShellResult;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

/**
 * Ganymed-based ssh client.  Tries to be simple and only offer a minimum feature set.
 * Converts IOException to RuntimeException.
 * Assumes the sshTarget login produces a bash shell.
 */
@Lazy
@Component
@Scope("prototype")
public class SshClient
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SshClient.class);

  @Autowired
  private SshConnectionFactory sshConnectionFactory;

  private SshTarget sshTarget;
  private Connection connection;

  public SshClient()
  {
  }

  /**
   * Initializes the SshClient, which means internally connecting and authenticating to the given host.
   */
  public SshClient init(SshTarget sshTarget)
  {
    this.sshTarget = sshTarget;
    final String hostname = sshTarget.getHostname();
    final String username = sshTarget.getUsername();
    final int connectTimeout = (int) sshTarget.getSoTimeoutMilliseconds();
    final int kexTimeout = (int) sshTarget.getKeyExchangeTimeoutMilliseconds();
    connection = sshConnectionFactory.create(hostname);
    boolean authenticated = false;
    try
    {
      connection.connect(null, connectTimeout, kexTimeout);//TODO - specify ServerHostKeyVerifier
      authenticated = connection.authenticateWithPassword(username, sshTarget.getPassword());
    }
    catch (IOException e)
    {
      throw new RuntimeException("Failed to make ssh connection to hostname '" + hostname + "'", e);
    }
    if (!authenticated)
    {
      throw new RuntimeException("Failed to authenticate ssh to hostname '" + hostname + "' as user '" + username + "'");
    }
    return this;
  }

  /**
   * Returns a string that describes the sshClient, for logging purposes.
   */
  private String context()
  {
    return "[" + sshTarget.getUsername() + "@" + sshTarget.getHostname() + "]: ";
  }

  /**
   * Executes the command and returns the stdout/stderr as a combined string.
   */
  public ShellResult execCommand(String command)
  {
    String wrappedCommand = "(" + command + ") 2>&1"; //Concats stderr to stdout; assumes bash shell.
    LOGGER.debug(context() + "Executing command '" + wrappedCommand + "'");
    StopWatch stopWatch = new StopWatch();
    Session session = null;
    try
    {
      stopWatch.start();
      session = connection.openSession();
      session.execCommand(wrappedCommand);
      return makeResult(session);
    }
    catch (Throwable e)
    {
      // Technically the above should only throw IOException.  However other exceptions are possible,
      // such as NullPointerException, and it would be a shame to have captured no output in such an event.
      try
      {
        LOGGER.error("Error in command: " + wrappedCommand);
        LOGGER.error("Command output:\n" + streamToString(session.getStdout()));
        LOGGER.error("Command exitValue: " + session.getExitStatus());
        LOGGER.error("Command exitSignal: " + session.getExitSignal());
      }
      catch (Throwable e2)
      {
        LOGGER.error("Unable to capture full results from ssh session :(");
      }
      throw new RuntimeException(context() + "Error executing command '" + wrappedCommand + "', time elapsed: " + stopWatch, e);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug(context() + "Time elapsed: " + stopWatch);
      if (session != null)
      {
        session.close();
      }
    }
  }

  private String streamToString(InputStream inputStream) throws IOException
  {
    return IOUtils.toString(inputStream);
  }

  private ShellResult makeResult(Session session) throws IOException
  {
    String output = streamToString(session.getStdout());
    Integer exitValue = session.getExitStatus();
    int exitValueAsInt = exitValue == null ? Integer.MIN_VALUE : exitValue;
    return new ShellResult(output, exitValueAsInt);
  }
}
