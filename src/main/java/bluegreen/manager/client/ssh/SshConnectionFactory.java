package bluegreen.manager.client.ssh;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.ssh2.Connection;

/**
 * Can create a Ganymed Connection object.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Lazy
@Component
public class SshConnectionFactory
{
  public Connection create(String hostname)
  {
    return new Connection(hostname);
  }
}
