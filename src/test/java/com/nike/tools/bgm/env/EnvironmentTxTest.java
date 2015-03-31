package com.nike.tools.bgm.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nike.tools.bgm.model.dao.EnvironmentDAO;
import com.nike.tools.bgm.model.domain.Application;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.Environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests the ability to perform transactions of environment objects.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentTxTest
{
  private static final String GOOD_ENVNAME1 = "GOOD_ENV1";
  private static final String GOOD_ENVNAME2 = "GOOD_ENV2";
  private static final String BAD_ENVNAME1 = "BAD_ENV1";
  private static final String BAD_ENVNAME2 = "BAD_ENV2";
  private static final Environment GOOD_ENV1 = new Environment();
  private static final Environment GOOD_ENV2 = new Environment();

  static
  {
    GOOD_ENV1.setEnvName(GOOD_ENVNAME1);
    GOOD_ENV2.setEnvName(GOOD_ENVNAME2);

    ApplicationVm applicationVm = new ApplicationVm();
    applicationVm.setHostname("theVmhost");
    Application application = new Application();
    application.setHostname("thehost");
    applicationVm.setApplications(Arrays.asList(application));
    GOOD_ENV1.setApplicationVms(Arrays.asList(applicationVm));
  }

  private static final String[] GOOD_ENVNAMES = { GOOD_ENVNAME1, GOOD_ENVNAME2 };
  private static final String[] BAD_ENVNAMES = { BAD_ENVNAME1, BAD_ENVNAME2 };
  private static final List<Environment> GOOD_ENVS = new ArrayList<Environment>(Arrays.asList(GOOD_ENV1, GOOD_ENV2));

  @InjectMocks
  private EnvironmentTx environmentTx;

  @Mock
  private EnvironmentDAO environmentDAO;

  @Before
  public void setUp()
  {
    when(environmentDAO.findNamedEnv(GOOD_ENVNAME1)).thenReturn(GOOD_ENV1);
    when(environmentDAO.findNamedEnvs(GOOD_ENVNAMES)).thenReturn(GOOD_ENVS);
    when(environmentDAO.findNamedEnvs(BAD_ENVNAMES)).thenReturn(null);
  }

  /**
   * Tests the ability to identify "good" named environments that "really exist."
   */
  @Test
  public void testCheckIfEnvNamesExist_Good()
  {
    boolean[] exists = environmentTx.checkIfEnvNamesExist(GOOD_ENVNAMES);
    assertEquals(2, exists.length);
    assertTrue(exists[0]);
    assertTrue(exists[1]);
  }

  /**
   * Tests the ability to identify "bad" environments that don't exist based on a name lookup.
   */
  @Test
  public void testCheckIfEnvNamesExist_Bad()
  {
    boolean[] exists = environmentTx.checkIfEnvNamesExist(BAD_ENVNAMES);
    assertEquals(2, exists.length);
    assertFalse(exists[0]);
    assertFalse(exists[1]);
  }

  /**
   * Lacking a hibernate session, this is not really a good test.
   */
  @Test
  public void testActiveLoadEnvironmentAndApplications()
  {
    Environment environment = environmentTx.activeLoadEnvironmentAndApplications(GOOD_ENVNAME1);
    assertNotNull(environment.getApplicationVms().get(0).getApplications().get(0));
  }
}
