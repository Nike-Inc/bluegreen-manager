package com.nike.tools.bgm.substituter;

import java.util.HashMap;

import org.junit.Test;

import static com.nike.tools.bgm.substituter.StringSubstituter.BLEEP;
import static com.nike.tools.bgm.substituter.StringSubstituter.CLOSE_EXPURGATE;
import static com.nike.tools.bgm.substituter.StringSubstituter.CLOSE_SUBSTITUTE;
import static com.nike.tools.bgm.substituter.StringSubstituter.OPEN_EXPURGATE;
import static com.nike.tools.bgm.substituter.StringSubstituter.OPEN_SUBSTITUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class StringSubstituterBaseImplTest
{
  private static final String KEY1 = "arg1";
  private static final String KEY2 = "arg2";
  private static final String VALUE1 = "hello";
  private static final String VALUE2 = "world";
  private static final String TEMPLATE_NO_VARS = "runCommand --arg1 value1 --arg2 value2";
  private static final String TEMPLATE_PUBLIC = "runCommand --arg1 " + OPEN_SUBSTITUTE + KEY1 + CLOSE_SUBSTITUTE
      + " --arg2 value2";
  private static final String TEMPLATE_EXPURGATE = "runCommand --arg1 " + OPEN_EXPURGATE + KEY1 + CLOSE_EXPURGATE
      + " --arg2 " + OPEN_SUBSTITUTE + KEY2 + CLOSE_SUBSTITUTE;

  private FakeStringSubstituterBaseImpl stringSubstituterBaseImpl = new FakeStringSubstituterBaseImpl();

  /**
   * Substitutions not initialized: error.
   */
  @Test(expected = IllegalStateException.class)
  public void testSubstituteVariables_Uninitialized()
  {
    stringSubstituterBaseImpl.substituteVariables(TEMPLATE_NO_VARS);
  }

  /**
   * One regular variable expression is matched.
   */
  @Test
  public void testSubstituteVariables_PublicMatch()
  {
    stringSubstituterBaseImpl.addSubstitution(KEY1, VALUE1);
    SubstituterResult result = stringSubstituterBaseImpl.substituteVariables(TEMPLATE_PUBLIC);
    assertEquals(result.getSubstituted(), result.getExpurgated());
    assertNotEquals(TEMPLATE_PUBLIC, result.getSubstituted());
    assertTrue(result.getSubstituted().contains("--arg1 " + VALUE1));
  }

  /**
   * Variables not matched: result is unchanged.
   */
  @Test
  public void testSubstituteVariables_PublicNoMatch()
  {
    stringSubstituterBaseImpl.addSubstitution(KEY2, VALUE2);
    SubstituterResult result = stringSubstituterBaseImpl.substituteVariables(TEMPLATE_PUBLIC);
    assertEquals(result.getSubstituted(), result.getExpurgated());
    assertEquals(TEMPLATE_PUBLIC, result.getSubstituted());
    assertFalse(result.getSubstituted().contains(VALUE2));
  }

  /**
   * Expurgated variable expression is matched, so substituted != expurgated.
   */
  @Test
  public void testSubstituteVariables_ExpurgateMatch()
  {
    stringSubstituterBaseImpl.addSubstitution(KEY1, VALUE1);
    stringSubstituterBaseImpl.addSubstitution(KEY2, VALUE2);
    SubstituterResult result = stringSubstituterBaseImpl.substituteVariables(TEMPLATE_EXPURGATE);
    assertNotEquals(result.getSubstituted(), result.getExpurgated());
    assertNotEquals(TEMPLATE_EXPURGATE, result.getSubstituted());
    assertNotEquals(TEMPLATE_EXPURGATE, result.getExpurgated());
    assertTrue(result.getSubstituted().contains("--arg1 " + VALUE1));
    assertTrue(result.getSubstituted().contains("--arg2 " + VALUE2));
    assertTrue(result.getExpurgated().contains("--arg1 " + BLEEP));
    assertTrue(result.getExpurgated().contains("--arg2 " + VALUE2));
  }

  private static class FakeStringSubstituterBaseImpl extends StringSubstituterBaseImpl
  {
    @Override
    public void loadDataModel()
    {
      throw new UnsupportedOperationException(); // Not invoked by these tests.
    }

    public void addSubstitution(String key, String value)
    {
      if (substitutions == null)
      {
        substitutions = new HashMap<String, String>();
      }
      substitutions.put(key, value);
    }
  }
}