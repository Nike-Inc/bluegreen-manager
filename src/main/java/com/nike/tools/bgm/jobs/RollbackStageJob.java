package com.nike.tools.bgm.jobs;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;

/**
 * Tears down the stage environment, and the test database.
 * This is a "rollback" of the prior stagingDeploy, meaning that that there was no goLive and the user wants to keep
 * the original live env.
 * <p/>
 * This is 99% a one-env job, but we need read access to the live env so that the rds delete task can make the name
 * of the bluegreen db snapshot.  Don't worry, we won't make any changes to the live env.
 */
@Lazy
@Component
public class RollbackStageJob extends TeardownJob
{
  public RollbackStageJob(String commandLine, boolean noop, boolean force,
                          JobHistory oldJobHistory, String deleteStageEnvName,
                          String liveEnvName, List<String> stopServices)
  {
    super(commandLine, noop, force, oldJobHistory, deleteStageEnvName, liveEnvName, stopServices);
  }
}
