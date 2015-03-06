package com.nike.tools.bgm.model.constraints;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint applied to the LogicalDatabase entity.  Specifically the constraint that the logical's
 * two physicals must be distinct.
 */
@Constraint(validatedBy = LogicalDatabaseValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface LogicalDatabaseConstraint
{
  String message() default "Logical database ${validatedValue.logicalName} (id ${validatedValue.logicalId}) "
      + "uses same physical database for both 'live' and 'other' physical, but these must be distinct.\n"
      + "  livePhysicalDatabase: ${validatedValue.livePhysicalDatabase}\n"
      + "  otherPhysicalDatabase: ${validatedValue.otherPhysicalDatabase}";

  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };

}
