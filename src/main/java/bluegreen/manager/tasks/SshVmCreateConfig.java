package bluegreen.manager.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Configures the commands sent over ssh for vm creation.
 * <p/>
 * TODO - This class should be absorbed into a general purpose shell config
 */
@Lazy
@Component
public class SshVmCreateConfig
{
  @Value("${bluegreen.sshvmcreate.initial.command}")
  private String initialCommand;

  @Value("${bluegreen.sshvmcreate.initial.regexp.ipaddress}")
  private String initialRegexpIpaddress;

  @Value("${bluegreen.sshvmcreate.initial.regexp.hostname}")
  private String initialRegexpHostname;

  @Value("${bluegreen.sshvmcreate.followup.command}")
  private String followupCommand;

  @Value("${bluegreen.sshvmcreate.followup.regexp.done}")
  private String followupRegexpDone;

  @Value("${bluegreen.sshvmcreate.followup.regexp.error}")
  private String followupRegexpError;

  //Warning: don't try to examine exitValue for success/failure, our ssh library Ganymed does not reliably return it.

  public SshVmCreateConfig()
  {
  }

  public SshVmCreateConfig(String initialCommand,
                           String initialRegexpIpaddress,
                           String initialRegexpHostname,
                           String followupCommand, String followupRegexpDone, String followupRegexpError)
  {
    this.initialCommand = initialCommand;
    this.initialRegexpIpaddress = initialRegexpIpaddress;
    this.initialRegexpHostname = initialRegexpHostname;
    this.followupCommand = followupCommand;
    this.followupRegexpDone = followupRegexpDone;
    this.followupRegexpError = followupRegexpError;
  }

  public String getInitialCommand()
  {
    return initialCommand;
  }

  public void setInitialCommand(String initialCommand)
  {
    this.initialCommand = initialCommand;
  }

  public String getInitialRegexpIpaddress()
  {
    return initialRegexpIpaddress;
  }

  public void setInitialRegexpIpaddress(String initialRegexpIpaddress)
  {
    this.initialRegexpIpaddress = initialRegexpIpaddress;
  }

  public String getInitialRegexpHostname()
  {
    return initialRegexpHostname;
  }

  public void setInitialRegexpHostname(String initialRegexpHostname)
  {
    this.initialRegexpHostname = initialRegexpHostname;
  }

  public String getFollowupCommand()
  {
    return followupCommand;
  }

  public void setFollowupCommand(String followupCommand)
  {
    this.followupCommand = followupCommand;
  }

  public String getFollowupRegexpDone()
  {
    return followupRegexpDone;
  }

  public void setFollowupRegexpDone(String followupRegexpDone)
  {
    this.followupRegexpDone = followupRegexpDone;
  }

  public String getFollowupRegexpError()
  {
    return followupRegexpError;
  }

  public void setFollowupRegexpError(String followupRegexpError)
  {
    this.followupRegexpError = followupRegexpError;
  }
}
