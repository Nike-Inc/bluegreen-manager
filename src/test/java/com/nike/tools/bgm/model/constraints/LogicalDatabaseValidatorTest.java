package com.nike.tools.bgm.model.constraints;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogicalDatabaseValidatorTest
{
  private static final String DRIVER_MYSQL = "com.mysql.jdbc.Driver";
  private static final String URL1 = "jdbc:mysql://hello.nikedev.com:3306/hello";
  private static final String URL2 = "jdbc:mysql://world.nikedev.com:3306/world";
  private static final String USERNAME = "mysql_admin";
  private static final String PASSWORD = "secret";

  private static Validator validator;

  @BeforeClass
  public static void setUp()
  {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  /**
   * Asserts that the input logical passes validation.
   */
  private void validateExpectSuccess(LogicalDatabase logical)
  {
    Set<ConstraintViolation<LogicalDatabase>> constraintViolations = validator.validate(logical);
    assertEquals(0, constraintViolations.size());
  }

  /**
   * Creates the logical (if input is null), adds a new live physical to the given url, and returns the logical.
   */
  private LogicalDatabase setLivePhysical(LogicalDatabase logical, String url)
  {
    if (logical == null)
    {
      logical = new LogicalDatabase();
    }
    PhysicalDatabase physical = new PhysicalDatabase();
    physical.setConnectionValues(DRIVER_MYSQL, url, USERNAME, PASSWORD);
    logical.setLivePhysicalDatabase(physical);
    return logical;
  }

  /**
   * Creates the logical (if input is null), adds a new 'other' physical to the given url, and returns the logical.
   */
  private LogicalDatabase setOtherPhysical(LogicalDatabase logical, String url)
  {
    if (logical == null)
    {
      logical = new LogicalDatabase();
    }
    PhysicalDatabase physical = new PhysicalDatabase();
    physical.setConnectionValues(DRIVER_MYSQL, url, USERNAME, PASSWORD);
    logical.setOtherPhysicalDatabase(physical);
    return logical;
  }

  /**
   * Ok to have both physicals equal to null.
   */
  @Test
  public void testPhysicalsNullIsValid()
  {
    LogicalDatabase logical = new LogicalDatabase();
    validateExpectSuccess(logical);
  }

  /**
   * Ok when the live physical is real but the other physical is null.
   */
  @Test
  public void testJustLivePhysicalIsValid()
  {
    LogicalDatabase logical = setLivePhysical(null, URL1);
    validateExpectSuccess(logical);
  }

  /**
   * Ok when the other physical is real but the live physical is null.
   */
  @Test
  public void testJustOtherPhysicalIsValid()
  {
    LogicalDatabase logical = setOtherPhysical(null, URL1);
    validateExpectSuccess(logical);
  }

  /**
   * Ok when both physicals are real and distinct.
   */
  @Test
  public void testDistinctRealPhysicalsAreValid()
  {
    /*
     * The two physicals have same physicalId 0, but different url.
     */
    LogicalDatabase logical = setLivePhysical(null, URL1);
    setOtherPhysical(logical, URL2);
    validateExpectSuccess(logical);
  }

  @Test
  public void testEquivalentPhysicalsAreInvalid()
  {
    LogicalDatabase logical = setLivePhysical(null, URL1);
    setOtherPhysical(logical, URL1);

    Set<ConstraintViolation<LogicalDatabase>> constraintViolations = validator.validate(logical);
    assertEquals(1, constraintViolations.size());
    String errorMessage = constraintViolations.iterator().next().getMessage();
    System.out.println("testEquivalentPhysicalsAreInvalid: " + errorMessage);
    assertTrue(errorMessage.contains("same physical database"));
  }
}