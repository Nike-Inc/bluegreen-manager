package bluegreen.manager.client.app;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import bluegreen.manager.client.http.ExecutorFactory;
import bluegreen.manager.client.http.HttpHelper;
import bluegreen.manager.client.http.HttpMethodType;
import bluegreen.manager.model.domain.Application;
import bluegreen.manager.utils.ThreadSleeper;

/**
 * HTTP client that makes requests to a blue-green compliant application, and knows to try again if
 * it gets a lock error.
 * <p/>
 * TODO - start capturing http response code, need to know if client has thrown an exception or responded non-200
 */
public class ApplicationClient
{
  static final int MAX_NUM_TRIES = 3;
  private static final long RETRY_DELAY_MILLISECONDS = 5000L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationClient.class);
  private static final String PARAMNAME_AUTHUSERNAME = "username";
  private static final String PARAMNAME_AUTHPASSWORD = "password";

  private ExecutorFactory executorFactory;
  private HttpHelper httpHelper;
  private Gson gson;
  private ThreadSleeper threadSleeper;
  private String applicationUsername;
  private String applicationPassword;

  public ApplicationClient(ExecutorFactory executorFactory,
                           HttpHelper httpHelper,
                           Gson gson,
                           ThreadSleeper threadSleeper,
                           String applicationUsername, String applicationPassword)
  {
    this.executorFactory = executorFactory;
    this.httpHelper = httpHelper;
    this.gson = gson;
    this.threadSleeper = threadSleeper;
    this.applicationUsername = applicationUsername;
    this.applicationPassword = applicationPassword;
  }

  /**
   * Initializes an http communication session with the application.
   */
  public ApplicationSession authenticate(Application application)
  {
    String uri = application.makeHostnameUri() + "/" + DbFreezeRest.POST_LOGIN;
    Executor httpExecutor = executorFactory.makeExecutor();
    CookieStore cookieStore = new BasicCookieStore();
    httpExecutor.cookieStore(cookieStore);
    NameValuePair[] authParams = new NameValuePair[] {
        new BasicNameValuePair(PARAMNAME_AUTHUSERNAME, applicationUsername),
        new BasicNameValuePair(PARAMNAME_AUTHPASSWORD, applicationPassword)
    };
    httpHelper.postAuthForCookie(httpExecutor, uri, authParams);
    return new ApplicationSession(httpExecutor, cookieStore);
  }

  /**
   * Requests dbfreeze progress from the application.
   * <p/>
   * Tries up to MAX_NUM_TRIES times to get a non-null response with no lock error.  Try-messaging can include an
   * optional outerTryNum if caller is in its own for-loop.
   */
  public DbFreezeProgress getDbFreezeProgress(Application application, ApplicationSession session, Integer outerTryNum)
  {
    return (DbFreezeProgress) requestWithRetry(application, session, HttpMethodType.GET,
        DbFreezeRest.GET_DB_FREEZE_PROGRESS, DbFreezeProgress.class, outerTryNum);
  }

  /**
   * Requests that the application enter/exit a dbfreeze, and returns initial progress.
   * <p/>
   * Tries up to MAX_NUM_TRIES times to get a non-null response with no lock error.  Try-messaging can include an
   * optional outerTryNum if caller is in its own for-loop.
   */
  public DbFreezeProgress putRequestTransition(Application application,
                                               ApplicationSession session,
                                               String transitionMethodPath,
                                               Integer outerTryNum)
  {
    return (DbFreezeProgress) requestWithRetry(application, session, HttpMethodType.PUT,
        transitionMethodPath, DbFreezeProgress.class, outerTryNum);
  }

  /**
   * Requests that the application discover its database.
   * <p/>
   * Tries up to MAX_NUM_TRIES times to get a non-null response with no lock error.  Try-messaging can include an
   * optional outerTryNum if caller is in its own for-loop.
   */
  public DiscoveryResult putDiscoverDb(Application application, ApplicationSession session, Integer outerTryNum)
  {
    return (DiscoveryResult) requestWithRetry(application, session, HttpMethodType.PUT,
        DbFreezeRest.PUT_DISCOVER_DB, DiscoveryResult.class, outerTryNum);
  }

  /**
   * Makes an application request that responds with a Lockable.  If the application returns a lock error, then
   * the client waits a bit and tries again.
   */
  Lockable requestWithRetry(Application application, ApplicationSession session, HttpMethodType httpMethodType,
                            String methodPath, Class<? extends Lockable> responseClass, Integer outerTryNum)
  {
    String uri = application.makeHostnameUri() + "/" + methodPath;
    int tryNum = 0;
    Lockable response = null;
    while (tryNum < MAX_NUM_TRIES)
    {
      response = tryRequest(httpMethodType, session, uri, responseClass, tryNum, outerTryNum);
      /*
       * TODO - in case of null, should check http response code.  Might not want to retry.
       */
      if (response == null || response.isLockError())
      {
        if (++tryNum < MAX_NUM_TRIES)
        {
          sleep();
        }
        else
        {
          LOGGER.error("Request failed after " + MAX_NUM_TRIES + " tries, final response: " + response);
        }
      }
      else
      {
        break;
      }
    }
    return response;
  }

  /**
   * Makes an application request that responds with json, and parses the json to a Lockable.
   */
  Lockable tryRequest(HttpMethodType httpMethodType, ApplicationSession session, String uri,
                      Class<? extends Lockable> responseClass, int tryNum, Integer outerTryNum)
  {
    Lockable response = null;
    String tryNumString = tryNumString(tryNum, outerTryNum);
    LOGGER.debug(tryNumString + " " + httpMethodType + " " + uri);
    String json = httpExecute(httpMethodType, session, uri);
    LOGGER.debug("Response: " + json);
    response = gson.fromJson(json, responseClass);
    if (response == null)
    {
      LOGGER.warn(tryNumString + " null response parsed from " + httpMethodType + " " + uri + " (raw response content: " + json + ")");
    }
    else if (response.isLockError())
    {
      LOGGER.info(tryNumString + " received lock error from " + httpMethodType + " " + uri);
    }
    return response;
  }

  /**
   * Returns a little string representing the current try, optionally prefixed by an outer try index in case
   * the ApplicationClient is being invoked by a caller in its own for-loop.
   */
  private String tryNumString(int tryNum, Integer outerTryNum)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Try #");
    if (outerTryNum != null)
    {
      sb.append(outerTryNum);
      sb.append(".");
    }
    sb.append(tryNum);
    return sb.toString();
  }

  /**
   * Invokes the httpExecutor on the uri, for the given http method.  Returns the response body as a string.
   */
  private String httpExecute(HttpMethodType httpMethodType, ApplicationSession session, String uri)
  {
    switch (httpMethodType)
    {
      case GET:
        return httpHelper.executeGet(session.getHttpExecutor(), uri);
      case PUT:
        return httpHelper.executePut(session.getHttpExecutor(), uri);
      default:
        throw new UnsupportedOperationException("Not expecting to send a '" + httpMethodType + "' request to a bluegreen application");
    }
  }

  /**
   * Sleeps for the try delay, and catches interrupt exceptions.
   */
  private void sleep()
  {
    LOGGER.debug("Going to sleep, will try again");
    try
    {
      threadSleeper.sleep(RETRY_DELAY_MILLISECONDS);
    }
    catch (InterruptedException e) //NOSONAR
    {
      LOGGER.warn("Sleep was interrupted");
    }
  }

  // Test purposes only
  void setGson(Gson gson)
  {
    this.gson = gson;
  }
}
