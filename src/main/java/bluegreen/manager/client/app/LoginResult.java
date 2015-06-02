package bluegreen.manager.client.app;

/**
 * Returned by a bluegreen-compliant login endpoint.
 */
public class LoginResult
{
  private boolean isLoggedIn;

  public LoginResult()
  {
  }

  public LoginResult(boolean isLoggedIn)
  {
    this.isLoggedIn = isLoggedIn;
  }

  public boolean isLoggedIn()
  {
    return isLoggedIn;
  }

  public void setLoggedIn(boolean isLoggedIn)
  {
    this.isLoggedIn = isLoggedIn;
  }
}
