package bluegreen.manager.client.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import bluegreen.manager.client.http.ExecutorFactory;
import bluegreen.manager.client.http.HttpHelper;
import bluegreen.manager.utils.ThreadSleeper;

/**
 * Creates client objects that can communicate with bluegreen applications.
 */
@Lazy
@Component
public class ApplicationClientFactory
{
  @Autowired
  private ExecutorFactory executorFactory;

  @Autowired
  private HttpHelper httpHelper;

  @Autowired
  private Gson gson;

  @Autowired
  private ThreadSleeper threadSleeper;

  /**
   * Creates a client that can communicate with a bluegreen application, using the specified credentials.
   */
  public ApplicationClient create(String applicationUsername, String applicationPassword)
  {
    return new ApplicationClient(executorFactory, httpHelper, gson, threadSleeper, applicationUsername, applicationPassword);
  }
}
