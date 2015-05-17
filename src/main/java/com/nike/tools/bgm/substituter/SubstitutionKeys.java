package com.nike.tools.bgm.substituter;

/**
 * Known keys for key/value substitution in shell commands.
 * When used in a command template, the key is enclosed with %{..} or %{{..}}.
 */
public interface SubstitutionKeys
{
  /**
   * Variable to be substituted with the name of the target bluegreen environment.
   */
  String ENV = "env";

  /**
   * Variable to be substituted with the name of the target bluegreen environment.
   * <p/>
   * TODO - Eliminate this one and only use ENV.
   */
  String ENV_NAME = "envName";

  /**
   * Variable to be substituted with the application vm hostname in a one-env context.
   * (Assumes there is exactly 1 applicationVm.)
   */
  String VM_HOSTNAME = "vmHostname";

  /**
   * Variable to be substituted with the application vm hostname in a one-env context.
   * (Assumes there is exactly 1 applicationVm.)
   * <p/>
   * TODO - Eliminate this one and only use VM_HOSTNAME.
   */
  String HOSTNAME = "hostname";

  /**
   * Variable to be substituted with the name of the live environment.
   */
  String LIVE_ENV = "liveEnv";

  /**
   * Variable to be substituted with the name of the stage environment.
   */
  String STAGE_ENV = "stageEnv";

  /**
   * Variable to be substituted with a comma-delimited list of four applicationVm properties:
   * liveHostname,liveIpAddress,stageHostname,stageIpAddress.
   * <p/>
   * Currently only supports mapping 1 vm from live to stage.
   */
  String APPLICATION_VM_MAP = "applicationVmMap";

  /**
   * Variable to be substituted with a comma-delimited list of two physicaldb properties:
   * livePhysicalInstName,stagePhysicalInstName
   * <p/>
   * Currently only supports mapping 1 physicaldb from live to stage.
   */
  String PHYSICAL_DB_MAP = "physicalDbMap";

  /**
   * Variable to be substituted with a comma-separated list of package names.
   */
  String PACKAGES = "packages";

  /**
   * Variable to be substituted with a comma-separated list of app names.
   */
  String STOP_SERVICES = "stopServices";
}
