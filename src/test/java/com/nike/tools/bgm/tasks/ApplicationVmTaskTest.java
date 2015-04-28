package com.nike.tools.bgm.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of ApplicationVmTask to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationVmTaskTest
{
  protected static final ApplicationVm FAKE_APPLICATION_VM = EnvironmentTestHelper.makeFakeApplicationVm();
  protected static final Environment FAKE_EMPTY_ENVIRONMENT = EnvironmentTestHelper.makeFakeEnvironment();

  @InjectMocks
  private ApplicationVmTask applicationVmTask = new ApplicationVmTask()
  {
    @Override
    public TaskStatus process(boolean noop)
    {
      throw new UnsupportedOperationException();//won't be tested here
    }
  };

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  /**
   * Calls the initializer for a task that creates a vm -- which should not already exist.
   */
  private void initCreateVm()
  {
    String envName = FAKE_EMPTY_ENVIRONMENT.getEnvName();
    when(mockEnvironmentTx.findNamedEnv(envName)).thenReturn(FAKE_EMPTY_ENVIRONMENT);
    applicationVmTask.assign(1, envName, true);
    applicationVmTask.loadDataModel();
  }

  /**
   * Calls the initializer for a task that modifies (or deletes) a vm -- which must already exist.
   */
  private void initModifyVm()
  {
    String envName = FAKE_APPLICATION_VM.getEnvironment().getEnvName();
    when(mockEnvironmentTx.findNamedEnv(envName)).thenReturn(FAKE_APPLICATION_VM.getEnvironment());
    applicationVmTask.assign(1, envName, false);
    applicationVmTask.loadDataModel();
  }

  /**
   * Vm Create task context should not initially include a hostname.
   */
  @Test
  public void testContext_Create()
  {
    initCreateVm();
    assertFalse(applicationVmTask.context().contains(", ")); //i.e. no hostname
  }

  /**
   * Vm Modify task context should initially include a hostname.
   */
  @Test
  public void testContext_Modify()
  {
    initModifyVm();
    assertTrue(applicationVmTask.context().contains(FAKE_APPLICATION_VM.getHostname()));
  }

}