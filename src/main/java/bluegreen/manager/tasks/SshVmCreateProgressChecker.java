package bluegreen.manager.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bluegreen.manager.client.ssh.SshClient;
import bluegreen.manager.client.ssh.SshTarget;
import bluegreen.manager.model.domain.ApplicationVm;
import bluegreen.manager.substituter.StringSubstituter;
import bluegreen.manager.substituter.StringSubstituterFactory;
import bluegreen.manager.substituter.SubstituterResult;
import static bluegreen.manager.substituter.SubstitutionKeys.HOSTNAME;
import bluegreen.manager.utils.ProgressChecker;
import bluegreen.manager.utils.RegexHelper;
import bluegreen.manager.utils.ShellResult;

/**
 * Knows how to check progress of vm creation initiated by an ssh command.
 */
public class SshVmCreateProgressChecker implements ProgressChecker<ApplicationVm>
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SshVmCreateProgressChecker.class);

  private ShellResult initialResult;
  private String logContext;
  private SshClient sshClient;
  private SshTarget sshTarget;
  private SshVmCreateConfig sshVmCreateConfig;
  private RegexHelper regexHelper;
  private StringSubstituterFactory stringSubstituterFactory;
  private String hostname; //vm created
  private String ipAddress; //vm created
  private Pattern initialPatternHostname;
  private Pattern initialPatternIpAddress;
  private Pattern followupPatternDone;
  private Pattern followupPatternError;
  private boolean done;
  private ApplicationVm result;

  public SshVmCreateProgressChecker(ShellResult initialResult,
                                    String logContext, SshClient sshClient,
                                    SshTarget sshTarget, SshVmCreateConfig sshVmCreateConfig,
                                    RegexHelper regexHelper,
                                    StringSubstituterFactory stringSubstituterFactory)
  {
    this.initialResult = initialResult;
    this.logContext = logContext;
    this.sshClient = sshClient;
    this.sshTarget = sshTarget;
    this.sshVmCreateConfig = sshVmCreateConfig;
    this.regexHelper = regexHelper;
    this.stringSubstituterFactory = stringSubstituterFactory;
    this.initialPatternHostname = Pattern.compile(sshVmCreateConfig.getInitialRegexpHostname());
    this.initialPatternIpAddress = Pattern.compile(sshVmCreateConfig.getInitialRegexpIpaddress());
    this.followupPatternDone = Pattern.compile(sshVmCreateConfig.getFollowupRegexpDone());
    this.followupPatternError = Pattern.compile(sshVmCreateConfig.getFollowupRegexpError());
  }

  /**
   * Returns a string that describes the ongoing operation, for logging purposes.
   * <p/>
   * To be used only after hostname/ipAddress are known.
   */
  String context()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(logContext);
    sb.append("SSH VM Creation for hostname '");
    sb.append(hostname);
    sb.append("', ipAddress ");
    sb.append(ipAddress);
    sb.append(": ");
    return sb.toString();
  }

  @Override
  public String getDescription()
  {
    return "SSH VM Creation by " + sshTarget.getUsername() + "@" + sshTarget.getHostname();
  }

  /**
   * Looks at initialResult to identify the new vm's hostname and ipaddress.
   * <p/>
   * This doesn't tell us if the vm is fully available.
   */
  @Override
  public void initialCheck()
  {
    if (StringUtils.isBlank(initialResult.getOutput()))
    {
      throw new RuntimeException("Blank initial output from " + getDescription());
    }
    LOGGER.debug("Initial output from " + getDescription() + ":\n" + initialResult.describe());
    hostname = getRequiredCapture("hostname", initialResult.getOutput(), initialPatternHostname);
    ipAddress = getRequiredCapture("ipAddress", initialResult.getOutput(), initialPatternIpAddress);
    LOGGER.info(context() + "STARTED");
  }

  /**
   * Returns the first capture group in the first line of output where the pattern is found.
   * <p/>
   * Never returns a blank string - throws if matcher cannot find it.
   */
  private String getRequiredCapture(String captureName, String output, Pattern pattern)
  {
    String value = regexHelper.matcherFirstCapture(output, pattern, 1);
    if (value != null)
    {
      return value;
    }
    else
    {
      throw new RuntimeException(logContext + "Could not find result value '" + captureName + "' in initial output");
    }
  }

  /**
   * Communicates using the sshClient to check progress on vm creation.
   */
  @Override
  public void followupCheck(int waitNum)
  {
    SubstituterResult command = substituteFollowupVariables(sshVmCreateConfig.getFollowupCommand());
    ShellResult followupResult = sshClient.execCommand(command);
    String followupOutput = followupResult.getOutput();
    LOGGER.debug("SSH VM Creation state after wait#" + waitNum + ": " + followupOutput);
    if (regexHelper.matcherFind(followupOutput, followupPatternError))
    {
      throw new RuntimeException(context() + "FAILED: " + followupOutput);
    }
    if (regexHelper.matcherFind(followupOutput, followupPatternDone))
    {
      done = true;
      result = makeApplicationVm();
    }
  }

  /**
   * Substitutes %{..} variables in the template commmand, returns the result.
   */
  private SubstituterResult substituteFollowupVariables(String template)
  {
    Map<String, String> substitutions = new HashMap<String, String>();
    substitutions.put(HOSTNAME, hostname);
    StringSubstituter stringSubstituter = stringSubstituterFactory.createZero(substitutions);
    stringSubstituter.loadDataModel();
    return stringSubstituter.substituteVariables(template);
  }

  /**
   * Makes a transient ApplicationVm entity based on hostname and ipAddress (already known to be non-blank).
   * Whoever is waiting for our progress result will associate this to an environment.
   */
  private ApplicationVm makeApplicationVm()
  {
    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setHostname(hostname);
    applicationVm.setIpAddress(ipAddress);
    return applicationVm;
  }

  @Override
  public boolean isDone()
  {
    return done;
  }

  /**
   * Non-null if the ApplicationVm has become available prior to timeout.
   */
  @Override
  public ApplicationVm getResult()
  {
    return result;
  }

  /**
   * Simply logs the timeout and returns null.
   */
  @Override
  public ApplicationVm timeout()
  {
    LOGGER.error(context() + " failed to become available prior to timeout");
    return null;
  }

  //Test purposes only
  String getHostname()
  {
    return hostname;
  }

  //Test purposes only
  String getIpAddress()
  {
    return ipAddress;
  }
}
