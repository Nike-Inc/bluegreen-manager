package com.nike.tools.bgm.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.TaskStatus;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ability of TwoEnvTask to get environmental info.
 */
@RunWith(MockitoJUnitRunner.class)
public class TwoEnvTaskTest
{
  private static final Environment FAKE_LIVE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(0);
  private static final Environment FAKE_STAGE_ENV = EnvironmentTestHelper.makeFakeFullEnvironment(1);

  @InjectMocks
  private TwoEnvTask twoEnvTask = new TwoEnvTask()
  {
    @Override
    public TaskStatus process(boolean noop)
    {
      throw new UnsupportedOperationException();//won't be tested here
    }
  };

  @Mock
  protected EnvironmentTx mockEnvironmentTx;

  @Before
  public void setUp()
  {
    when(mockEnvironmentTx.findNamedEnv(FAKE_LIVE_ENV.getEnvName())).thenReturn(FAKE_LIVE_ENV);
    when(mockEnvironmentTx.findNamedEnv(FAKE_STAGE_ENV.getEnvName())).thenReturn(FAKE_STAGE_ENV);
  }

  @Test
  public void testLoadDataModel()
  {
    twoEnvTask.assign(1, FAKE_LIVE_ENV.getEnvName(), FAKE_STAGE_ENV.getEnvName());
    twoEnvTask.loadDataModel();
    verify(mockEnvironmentTx, times(2)).findNamedEnv(anyString());
    //Not much else to assert except that we got here and it didn't throw
  }

}
