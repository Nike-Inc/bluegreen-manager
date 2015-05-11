package com.nike.tools.bgm.jobs;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.model.domain.JobHistory;

/**
 * Tears down the old live environment, and the test database used by the former stage env.
 * This is a "commit" of the prior goLive, meaning that only the new live env (not touched here) will remain.
 * <p/>
 * This is a one-env job, because the delete env is the same one originally used to make the bluegreen db snapshot.
 */
@Lazy
@Component
public class TeardownCommitJob extends TeardownJob
{
  public TeardownCommitJob(String commandLine, boolean noop, boolean force,
                           JobHistory oldJobHistory, String deleteOldLiveEnvName,
                           List<String> stopServices)
  {
    super(commandLine, noop, force, oldJobHistory, deleteOldLiveEnvName, deleteOldLiveEnvName, stopServices);
  }
}
