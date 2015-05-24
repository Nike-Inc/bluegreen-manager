package bluegreen.manager.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.model.tx.EnvLoaderFactory;
import bluegreen.manager.model.tx.OneEnvLoader;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of ApplicationVmTask to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationVmTaskTest
{
  private static final String FAKE_ENV_NAME = "theEnv";

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
  protected EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(FAKE_ENV_NAME)).thenReturn(mockOneEnvLoader);
  }

  /**
   * Initialization when we want to create a vm.
   */
  private void initCreateVm()
  {
    applicationVmTask.assign(1, FAKE_ENV_NAME, true);
    applicationVmTask.loadDataModel();
    verify(mockOneEnvLoader).loadApplicationVm(true);
  }

  /**
   * Initialization when we want to modify/delete an existing vm.
   */
  private void initModifyVm()
  {
    applicationVmTask.assign(1, FAKE_ENV_NAME, false);
    applicationVmTask.loadDataModel();
    verify(mockOneEnvLoader).loadApplicationVm(false);
  }

  /**
   * Tests that we use the env loader for describing context.
   */
  @Test
  public void testContext_Create()
  {
    initCreateVm();
    String str = applicationVmTask.context();
    verify(mockOneEnvLoader).context();
  }

  /**
   * Tests that we use the env loader for describing context.
   */
  @Test
  public void testContext_Modify()
  {
    initModifyVm();
    String str = applicationVmTask.context();
    verify(mockOneEnvLoader).context();
  }

}