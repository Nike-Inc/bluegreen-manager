package bluegreen.manager.jobs;

import bluegreen.manager.model.domain.JobStatus;

/**
 * A job is a sequence of tasks that the bluegreen-manager can run.
 */
public interface Job
{
  /**
   * Executes the tasks.  Returns jobStatus or throws if error.
   */
  JobStatus process();

  /**
   * Returns job name, i.e. what kind of job.
   */
  String getName();

  /**
   * Returns the first environment on which the job operates.
   */
  String getEnv1();

  /**
   * Returns the second environment on which the job operates.  (Optional, may be null.)
   */
  String getEnv2();

  /**
   * Returns the command-line used to invoke this job.
   */
  String getCommandLine();
}
