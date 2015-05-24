package bluegreen.manager.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.client.app.DiscoveryResult;
import bluegreen.manager.client.app.PhysicalDatabase;
import bluegreen.manager.model.domain.TaskStatus;

/**
 * Pokes the applications in the requested environment so they discover their physical databases based on the latest
 * data model.
 * <p/>
 * Assumes the target apps have already been frozen, assumes the live db has already been linked to the target applications.
 * Currently assumes exactly 1 application.
 */
@Lazy
@Component
public class DiscoveryTask extends ApplicationTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryTask.class);

  @Override
  public TaskStatus process(boolean noop)
  {
    loadDataModel();
    initApplicationSession();
    pokeApplication(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  /**
   * Pokes the requested application to discover its physical databases based on the latest
   * data model.
   * <p/>
   * Sanity checks the application's discovery result, throws if any problem.
   */
  private void pokeApplication(boolean noop)
  {
    LOGGER.info(context() + "Poking application to discover its physical database" + noopRemark(noop));
    if (!noop)
    {
      DiscoveryResult result = applicationClient.putDiscoverDb(application, applicationSession, 0);
      checkResult(result);
      LOGGER.info("Application discovered " + result.getPhysicalDatabase().toString());
    }
  }

  /**
   * Sanity checks the discovery result.  Returns silently if ok.
   */
  void checkResult(DiscoveryResult result)
  {
    if (result == null)
    {
      discoveryError("Unexpected null result from application");
    }
    else if (result.isLockError())
    {
      discoveryError("Lock error, so application did not attempt discovery");
    }
    else if (StringUtils.isNotBlank(result.getDiscoveryError()))
    {
      discoveryError("Application incurred an error during discovery: " + result.getDiscoveryError());
    }
    else
    {
      checkDiscoveredPhysicalDatabase(result.getPhysicalDatabase());
    }
  }

  /**
   * Sanity checks the physicaldb discovered by the application.  Returns silently if ok.
   *
   * @param physicalDatabase Java class is from the client api package, not the bluegreen entity model.
   */
  private void checkDiscoveredPhysicalDatabase(PhysicalDatabase physicalDatabase)
  {
    if (physicalDatabase == null)
    {
      discoveryError("Application did not respond with the identity of its discovered database");
    }
    else if (!StringUtils.equals(physicalDatabase.getEnvName(), environment.getEnvName()))
    {
      physicalDatabaseError(physicalDatabase, "Physical database discovered by application is not in expected environment '"
          + environment.getEnvName() + "'");
    }
    else if (!physicalDatabase.isDbIsLive())
    {
      physicalDatabaseError(physicalDatabase, "Physical database discovered by application is supposed to be live!");
    }
  }

  private void physicalDatabaseError(PhysicalDatabase physicalDatabase, String message)
  {
    discoveryError(physicalDatabase.toString() + ": " + message);
  }

  private void discoveryError(String message)
  {
    throw new RuntimeException(context() + message);
  }
}
