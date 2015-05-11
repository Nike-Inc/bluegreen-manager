package com.nike.tools.bgm.model.tx;

import org.junit.Test;

import com.nike.tools.bgm.model.domain.Environment;
import com.nike.tools.bgm.model.domain.EnvironmentTestHelper;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;

import static org.junit.Assert.assertEquals;

public class EnvironmentHelperTest
{
  private EnvironmentHelper environmentHelper = new EnvironmentHelper();

  @Test
  public void testListOfNames()
  {
    Environment env = EnvironmentTestHelper.makeFakeEnvironment();
    PhysicalDatabase physicalDatabase1 = EnvironmentTestHelper.makeFakePhysicalDatabase(0, env);
    PhysicalDatabase physicalDatabase2 = EnvironmentTestHelper.makeFakePhysicalDatabase(1, env);
    assertEquals("logicaldb (physDb1 - jdbc:mysql://db-vm-1.com:3306/theDb), logicaldb (physDb2 - jdbc:mysql://db-vm-2.com:3306/theDb)",
        environmentHelper.listOfNames(env.getLogicalDatabases()));
  }
}
