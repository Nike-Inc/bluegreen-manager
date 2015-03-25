package com.nike.tools.bgm.client.app;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.fluent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.nike.tools.bgm.client.http.ExecutorFactory;
import com.nike.tools.bgm.client.http.HttpHelper;
import com.nike.tools.bgm.client.http.HttpMethodType;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.utils.ThreadSleeper;

/**
 * HTTP client that makes requests to a blue-green compliant application, and knows to try again if
 * it gets a lock error.
 * <p/>
 * TODO - start capturing http response code, need to know if client has thrown an exception or responded non-200
 */
@Lazy
@Component
public class ApplicationClient
{
  static final int MAX_NUM_TRIES = 3;
  private static final long RETRY_DELAY_MILLISECONDS = 5000L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationClient.class);

  @Autowired
  private ExecutorFactory executorFactory;

  @Autowired
  private HttpHelper httpHelper;

  @Autowired
  private Gson gson;

  @Autowired
  private ThreadSleeper threadSleeper;

  /**
   * Internal cache of http executors, specific to an application.
   */
  private Map<Application, Executor> executors = new HashMap<Application, Executor>();

  /**
   * Requests dbfreeze progress from the application.
   * <p/>
   * Tries up to MAX_NUM_TRIES times to get a non-null response with no lock error.
   */
  public DbFreezeProgress getDbFreezeProgress(Application application)
  {
    return (DbFreezeProgress) requestWithRetry(application, HttpMethodType.GET, DbFreezeRest.GET_DB_FREEZE_PROGRESS,
        DbFreezeProgress.class);
  }

  /**
   * Requests that the application start a dbfreeze, and returns initial progress.
   * <p/>
   * Tries up to MAX_NUM_TRIES times to get a non-null response with no lock error.
   */
  public DbFreezeProgress putEnterDbFreeze(Application application)
  {
    return (DbFreezeProgress) requestWithRetry(application, HttpMethodType.PUT, DbFreezeRest.PUT_ENTER_DB_FREEZE,
        DbFreezeProgress.class);
  }

  /**
   * Requests that the application discover its database.
   * <p/>
   * Tries up to MAX_NUM_TRIES times to get a non-null response with no lock error.
   */
  public DiscoveryResult putDiscoverDb(Application application)
  {
    return (DiscoveryResult) requestWithRetry(application, HttpMethodType.PUT, DbFreezeRest.PUT_DISCOVER_DB,
        DiscoveryResult.class);
  }

  /**
   * Makes an application request that responds with a Lockable.  If the application returns a lock error, then
   * the client waits a bit and tries again.
   */
  Lockable requestWithRetry(Application application, HttpMethodType httpMethodType, String methodPath,
                            Class<? extends Lockable> responseClass)
  {
    Executor httpExecutor = makeOrReuseAuthenticatedExecutor(application);
    String uri = application.makeHostnameUri() + "/" + methodPath;
    int tryNum = 0;
    Lockable response = null;
    while (tryNum < MAX_NUM_TRIES)
    {
      response = tryRequest(httpMethodType, httpExecutor, uri, responseClass, tryNum);
      /*
       * TODO - in case of null, should check http response code.  Might not want to retry.
       */
      if ((response == null || response.isLockError()))
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
  Lockable tryRequest(HttpMethodType httpMethodType, Executor httpExecutor, String uri,
                      Class<? extends Lockable> responseClass, int tryNum)
  {
    Lockable response = null;
    LOGGER.debug("Try #" + tryNum + " " + httpMethodType + " " + uri);
    String json = httpExecute(httpMethodType, httpExecutor, uri);
    LOGGER.debug("Response: " + json);
    response = gson.fromJson(json, responseClass);
    if (response == null)
    {
      LOGGER.warn("Try #" + tryNum + " null response parsed from " + httpMethodType + " " + uri + " (raw response content: " + json + ")");
    }
    else if (response.isLockError())
    {
      LOGGER.info("Try #" + tryNum + " received lock error from " + httpMethodType + " " + uri);
    }
    return response;
  }

  /**
   * Invokes the httpExecutor on the uri, for the given http method.  Returns the response body as a string.
   */
  private String httpExecute(HttpMethodType httpMethodType, Executor httpExecutor, String uri)
  {
    switch (httpMethodType)
    {
      case GET:
        return httpHelper.executeGet(httpExecutor, uri);
      case PUT:
        return httpHelper.executePut(httpExecutor, uri);
      default:
        throw new UnsupportedOperationException("Not expecting to send a '" + httpMethodType + "' request to a bluegreen application");
    }
  }

  /**
   * Looks up the authenticated executor for this application, or creates a new one if not found.
   */
  Executor makeOrReuseAuthenticatedExecutor(Application application)
  {
    if (executors.containsKey(application))
    {
      return executors.get(application);
    }
    else
    {
      Executor httpExecutor = executorFactory.makeAuthenticatedExecutor(application);
      executors.put(application, httpExecutor);
      return httpExecutor;
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
    catch (InterruptedException e)
    {
      LOGGER.warn("Sleep was interrupted");
    }
  }

  // Test purposes only
  public void setGson(Gson gson)
  {
    this.gson = gson;
  }
}
