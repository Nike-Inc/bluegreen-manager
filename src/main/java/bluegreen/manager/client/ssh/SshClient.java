package bluegreen.manager.client.ssh;

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

import bluegreen.manager.substituter.SubstituterResult;
import bluegreen.manager.utils.ShellResult;
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
      connection.connect(null, connectTimeout, kexTimeout);//Not specified: ServerHostKeyVerifier
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
   * <p/>
   * Technically it returns the exitValue as well, but our ssh library (Ganymed) is unreliable here.  I cannot figure
   * out why sometimes session.getExitStatus has a good value and other times is null, even when the remote vms have
   * the same configuration and are running the same command with presumably the same results.
   */
  public ShellResult execCommand(SubstituterResult command)
  {
    SubstituterResult wrappedCommand = wrapSubstituterResultForStdout(command);
    LOGGER.debug(context() + "Executing command '" + wrappedCommand.getExpurgated() + "'");
    StopWatch stopWatch = new StopWatch();
    Session session = null;
    try
    {
      stopWatch.start();
      session = connection.openSession();
      session.execCommand(wrappedCommand.getSubstituted());
      return makeResult(session);
    }
    catch (Throwable e)
    {
      // Technically the above should only throw IOException.  However other exceptions are possible,
      // such as NullPointerException, and it would be a shame to have captured no output in such an event.
      logSessionResultNoThrow(wrappedCommand.getExpurgated(), session);

      throw new RuntimeException(context() + "Error executing command '" + wrappedCommand.getExpurgated()
          + "', time elapsed: " + stopWatch, e);
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

  private SubstituterResult wrapSubstituterResultForStdout(SubstituterResult command)
  {
    return new SubstituterResult(wrapStringForStdout(command.getSubstituted()),
        wrapStringForStdout(command.getExpurgated()));
  }

  /**
   * Concats stderr to stdout; assumes bash shell.
   */
  private String wrapStringForStdout(String command)
  {
    return "(" + command + ") 2>&1";
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

  /**
   * Desperate attempt to log something for diagnostics purposes, without allowing the session to throw during logging.
   * The caller has already caught an exception, and it will be re-thrown as soon as this returns.
   */
  private void logSessionResultNoThrow(String command, Session session)
  {
    try
    {
      LOGGER.error("Error in command: " + command);
      LOGGER.error("Command output:\n" + streamToString(session.getStdout()));
      LOGGER.error("Command exitValue: " + session.getExitStatus());
      LOGGER.error("Command exitSignal: " + session.getExitSignal());
    }
    catch (Throwable e)
    {
      LOGGER.error("Unable to capture full results from ssh session :(", e);
    }
  }
}
