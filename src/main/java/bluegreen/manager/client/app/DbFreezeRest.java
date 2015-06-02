package bluegreen.manager.client.app;

/**
 * Names of the restful 'commands' on the api of a blue-green compliant application.
 */
public class DbFreezeRest
{
  private DbFreezeRest()
  {
    //Do not instantiate me
  }

  public static final String POST_LOGIN = "login";

  public static final String GET_DB_FREEZE_PROGRESS = "dbFreezeProgress";

  public static final String PUT_ENTER_DB_FREEZE = "enterDbFreeze";

  public static final String PUT_EXIT_DB_FREEZE = "exitDbFreeze";

  public static final String PUT_DISCOVER_DB = "discoverDb";
}
