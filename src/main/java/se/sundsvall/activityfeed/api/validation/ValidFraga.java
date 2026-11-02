package se.sundsvall.activityfeed.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import se.sundsvall.activityfeed.api.validation.impl.ValidFragaConstraintValidator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The rules of a query that span several fields: every party has a customer, an errand or both, pagination comes with
 * sorting, and startDatum is not after slutDatum. Each violation points at the field it concerns.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidFragaConstraintValidator.class)
public @interface ValidFraga {

	String message() default "frågan är inte korrekt ställd";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
