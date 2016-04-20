package bluegreen.manager.jobs;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import bluegreen.manager.model.domain.JobHistory;
import bluegreen.manager.tasks.DiscoveryTask;
import bluegreen.manager.tasks.FixedElbFlipEc2Task;
import bluegreen.manager.tasks.FreezeTask;
import bluegreen.manager.tasks.LocalShellTask;
import bluegreen.manager.tasks.ShellConfig;
import bluegreen.manager.tasks.SmokeTestTask;
import bluegreen.manager.tasks.SwapDatabasesTask;
import bluegreen.manager.tasks.Task;
import bluegreen.manager.tasks.ThawTask;

/**
 * Swaps the liveness of the old and new envs.  Old becomes live.
 * <p/>
 * stagingDeploy liveEnv becomes goLive oldLiveEnv.
 * stagingDeploy stageEnv becomes goLive newLiveEnv.
 */
@Lazy
@Component
public class GoLiveJob extends TaskSequenceJob
{
  @Autowired
  @Qualifier("swapDatabases")
  private ShellConfig swapDatabasesConfig;

  private String oldLiveEnvName;
  private String newLiveEnvName;
  private String fixedLbName;

  public GoLiveJob(String commandLine, boolean noop, boolean force,
                   JobHistory oldJobHistory, String oldLiveEnvName, String newLiveEnvName, String fixedLbName)
  {
    super(commandLine, noop, force, oldJobHistory);
    this.oldLiveEnvName = oldLiveEnvName;
    this.newLiveEnvName = newLiveEnvName;
    this.fixedLbName = fixedLbName;
  }

  /**
   * Instantiates the sequence of tasks for the go-live job.
   * <p/>
   * Is PostConstruct to have access to applicationContext.
   */
  @PostConstruct
  private void instantiateTasks()
  {
    int position = 1;
    List<Task> tasks = new ArrayList<Task>();
    tasks.add(applicationContext.getBean(FreezeTask.class).assignTransition(position++, newLiveEnvName));
    tasks.add(applicationContext.getBean(FreezeTask.class).assignTransition(position++, oldLiveEnvName));
    tasks.add(applicationContext.getBean(LocalShellTask.class).assign(position++, oldLiveEnvName, newLiveEnvName, swapDatabasesConfig, true));
    tasks.add(applicationContext.getBean(SwapDatabasesTask.class).assign(position++, oldLiveEnvName, newLiveEnvName));
    tasks.add(applicationContext.getBean(DiscoveryTask.class).assign(position++, newLiveEnvName));
    tasks.add(applicationContext.getBean(SmokeTestTask.class).assign(position++, newLiveEnvName));
    tasks.add(applicationContext.getBean(FixedElbFlipEc2Task.class).assign(position++, oldLiveEnvName, newLiveEnvName, fixedLbName));
    tasks.add(applicationContext.getBean(ThawTask.class).assignTransition(position++, newLiveEnvName));
    this.tasks = tasks;
  }

  @Override
  public String getEnv1()
  {
    return oldLiveEnvName;
  }

  @Override
  public String getEnv2()
  {
    return newLiveEnvName;
  }
}
