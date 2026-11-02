package se.sundsvall.activityfeed.api.validation.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.Behandling;
import se.sundsvall.activityfeed.api.model.Fraga;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.Paginering;
import se.sundsvall.activityfeed.api.model.Part;
import se.sundsvall.activityfeed.api.model.Sortering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.activityfeed.api.validation.impl.ValidFragaConstraintValidator.ERROR_EMPTY_PARTY;
import static se.sundsvall.activityfeed.api.validation.impl.ValidFragaConstraintValidator.ERROR_PAGINATION_WITHOUT_SORTING;
import static se.sundsvall.activityfeed.api.validation.impl.ValidFragaConstraintValidator.ERROR_START_AFTER_END;

/**
 * Runs the validator through Bean Validation, so the paths of the violations are checked too.
 */
class ValidFragaConstraintValidatorTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void setup() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@Test
	void validQuery() {
		final var query = query(customer())
			.withBehandling(Behandling.create()
				.withSortering(List.of(Sortering.create().withAttribut("TIDPUNKT").withStigande(true)))
				.withPaginering(Paginering.create().withOffset(0)))
			.withStartDatum("2026-09-01")
			.withSlutDatum("2026-09-01");

		assertThat(violations(query)).isEmpty();
	}

	@Test
	void nullAndMissingPartiesAreLeftToOtherConstraints() {
		final var constraintValidator = new ValidFragaConstraintValidator();

		assertThat(constraintValidator.isValid(null, null)).isTrue();
		assertThat(constraintValidator.isValid(Fraga.create(), null)).isTrue();
	}

	@Test
	void everyPartyNeedsCustomerOrErrand() {
		final var errand = Part.create().withArende(Arende.create().withIdentifierare("A-1").withTyp("Diarienummer"));

		assertThat(violations(query(customer(), Part.create(), null, errand))).containsExactlyInAnyOrder(
			tuple("parter[1]", ERROR_EMPTY_PARTY),
			tuple("parter[2]", ERROR_EMPTY_PARTY));
	}

	@Test
	void paginationNeedsSorting() {
		final var query = query(customer()).withBehandling(Behandling.create().withPaginering(Paginering.create().withOffset(0)));
		assertThat(violations(query)).containsExactly(tuple("behandling.paginering", ERROR_PAGINATION_WITHOUT_SORTING));

		query.getBehandling().withSortering(List.of());
		assertThat(violations(query)).containsExactly(tuple("behandling.paginering", ERROR_PAGINATION_WITHOUT_SORTING));

		query.withBehandling(Behandling.create());
		assertThat(violations(query)).isEmpty();
	}

	@Test
	void startDateMustNotBeAfterEndDate() {
		assertThat(violations(query(customer()).withStartDatum("2026-09-02").withSlutDatum("2026-09-01")))
			.containsExactly(tuple("startDatum", ERROR_START_AFTER_END));
		assertThat(violations(query(customer()).withStartDatum("2026-09-02"))).isEmpty();
		assertThat(violations(query(customer()).withSlutDatum("2026-09-01"))).isEmpty();
	}

	@Test
	void malformedDatesAreLeftToTheFieldPattern() {
		final var query = query(customer()).withStartDatum("2026-9-2").withSlutDatum("2026-09-01");

		assertThat(validator.validate(query)).extracting(ConstraintViolation::getMessage)
			.containsExactly("startDatum ska ha formatet ÅÅÅÅ-MM-DD");
	}

	private static List<Tuple> violations(final Fraga query) {
		return validator.validate(query).stream()
			.map(violation -> tuple(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
	}

	private static Fraga query(final Part... parties) {
		return Fraga.create().withParter(Arrays.asList(parties));
	}

	private static Part customer() {
		return Part.create().withKund(Kund.create().withIdentifierare("199009090000").withTyp("Personnummer"));
	}
}
