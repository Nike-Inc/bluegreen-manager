package bluegreen.manager.main;

/**
 * Thrown when cmdline args are invalid.
 */
public class CmdlineException extends RuntimeException
{
  public CmdlineException(String s)
  {
    super(s);
  }
}
