package com.nike.tools.bgm.main;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Parses command-line arguments and identifies (1) job name, (2) parameters.
 * <p/>
 * Assumption is the command-line will be of the form "java -jar thisjar someJobName --param1=val1 --param2=val2".
 */
@Component
public class ArgumentParser
{
  public static final String DOUBLE_HYPHEN = "--";

  private String jobName;

  private List<List<String>> parameters;

  private String commandLine;

  /**
   * Parses the given command-line arguments and locally saves the job name and parameters.
   * <p/>
   * Throws if any basic syntax errors.
   */
  public void parseArgs(String[] args)
  {
    jobName = null;
    parameters = null;
    commandLine = null;
    if (args != null && args.length > 0)
    {
      commandLine = StringUtils.join(args, " ");
      jobName = args[0];
      parameters = parseParameters(args);
    }
    else
    {
      throw new CmdlineException("Syntax Error: Please specify some arguments");
    }
  }

  /**
   * A jobName has been parsed already from args[0], so start parsing parameters from args[1] onwards.
   */
  private List<List<String>> parseParameters(String[] args)
  {
    int idx = 1;
    List<List<String>> parameters = new ArrayList<List<String>>();
    while (idx < args.length)
    {
      idx = parseNextParameter(args, idx, parameters);
    }
    return parameters;
  }

  /**
   * Starts parsing args at the given index, adds the next parameter ("--param" or "--paramname val1 val2")
   * to the input parameters list, and returns the next index.
   */
  private int parseNextParameter(String[] args, int idx, List<List<String>> parameters)
  {
    List<String> paramParts = parseParamName(args[idx], parameters);
    return parseParamValues(args, idx + 1, paramParts);
  }

  /**
   * Checks if the specified paramName is valid, and if so adds a new sublist of paramParts to the input parameters
   * (outer list) and returns the sublist.
   * <p/>
   * Caller can add param values to the sublist.
   */
  private List<String> parseParamName(String paramName, List<List<String>> parameters)
  {
    if (!paramName.startsWith(DOUBLE_HYPHEN))
    {
      throw new CmdlineException("Syntax Error at '" + paramName
          + "': parameter name must begin with '" + DOUBLE_HYPHEN + "'");
    }
    List<String> paramParts = new ArrayList<String>();
    paramParts.add(paramName.substring(DOUBLE_HYPHEN.length())); //param name without the hyphens
    parameters.add(paramParts);
    return paramParts;
  }

  /**
   * Starts at the specified index and adds args to the paramParts sublist, until the end of the current
   * parameter is reached.  (Denoted by end of args, or new hyphens.)  Returns the next index where parsing
   * should continue.
   */
  private int parseParamValues(String[] args, int idx, List<String> paramParts)
  {
    while (idx < args.length && !args[idx].startsWith(DOUBLE_HYPHEN))
    {
      paramParts.add(args[idx]);
      ++idx;
    }
    return idx;
  }

  public String getJobName()
  {
    return jobName;
  }

  public List<List<String>> getParameters()
  {
    return parameters;
  }

  public String getCommandLine()
  {
    return commandLine;
  }
}
