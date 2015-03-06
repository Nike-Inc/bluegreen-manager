package com.nike.tools.bgm.model.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.nike.tools.bgm.model.domain.LogicalDatabase;
import com.nike.tools.bgm.model.domain.PhysicalDatabase;

/**
 * Validates a LogicalDatabaseConstraint.  Specifically the constraint that the logical's two physicals must be distinct.
 */
public class LogicalDatabaseValidator implements ConstraintValidator<LogicalDatabaseConstraint, LogicalDatabase>
{
  @Override
  public void initialize(LogicalDatabaseConstraint logicalDatabaseConstraint)
  {
    //Nothing to do
  }

  /**
   * True if the logical database's two physicals are distinct.
   * <p/>
   * Using only the concept of field equivalence, NOT database identity, since new objects all have id=0 until an
   * indeterminate time after first persistence.  (JPA spec leaves it up to the provider.)
   * <p/>
   * Ok if both physicals are null, but not if the logical is null.
   */
  @Override
  public boolean isValid(LogicalDatabase logicalDatabase, ConstraintValidatorContext constraintValidatorContext)
  {
    if (logicalDatabase != null)
    {
      PhysicalDatabase livePhysical = logicalDatabase.getLivePhysicalDatabase();
      PhysicalDatabase otherPhysical = logicalDatabase.getOtherPhysicalDatabase();
      if (livePhysical == null || otherPhysical == null)
      {
        return true;
      }
      else
      {
        return !livePhysical.isEquivalentTo(otherPhysical);
      }
    }
    return false;
  }
}
