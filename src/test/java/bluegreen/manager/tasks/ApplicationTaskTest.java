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
 * Not much to test, since OneEnvLoader does 99% of the work now.
 * <p/>
 * Only other thing would be to test initApplicationSession, and other unit tests effectively do that.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationTaskTest
{
  private static final String FAKE_ENV_NAME = "theEnv";

  @InjectMocks
  private ApplicationTask applicationTask = new ApplicationTask()
  {
    @Override
    public TaskStatus process(boolean noop)
    {
      throw new UnsupportedOperationException();//won't be tested here
    }
  };

  @Mock
  private EnvLoaderFactory mockEnvLoaderFactory;

  @Mock
  private OneEnvLoader mockOneEnvLoader;

  @Before
  public void setUp()
  {
    when(mockEnvLoaderFactory.createOne(FAKE_ENV_NAME)).thenReturn(mockOneEnvLoader);
    applicationTask.assign(1, FAKE_ENV_NAME);
    applicationTask.loadDataModel();
    verify(mockOneEnvLoader).loadApplication();
  }

  /**
   * Tests that we use the env loader for describing context.
   */
  @Test
  public void testContext()
  {
    String str = applicationTask.context();
    verify(mockOneEnvLoader).context();
  }

}