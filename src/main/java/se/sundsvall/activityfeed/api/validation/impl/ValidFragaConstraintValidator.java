package se.sundsvall.activityfeed.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import se.sundsvall.activityfeed.api.model.Behandling;
import se.sundsvall.activityfeed.api.model.Fraga;
import se.sundsvall.activityfeed.api.validation.ValidFraga;

public class ValidFragaConstraintValidator implements ConstraintValidator<ValidFraga, Fraga> {

	static final String ERROR_EMPTY_PARTY = "Varje part måste innehålla kund, ärende eller båda";
	static final String ERROR_PAGINATION_WITHOUT_SORTING = "Paginering kräver att sortering anges";
	static final String ERROR_START_AFTER_END = "startDatum får inte vara efter slutDatum";

	/** The dates are only compared when both have the format that the fields themselves require. */
	private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	@Override
	public boolean isValid(final Fraga query, final ConstraintValidatorContext context) {
		if (query == null) {
			return true;
		}
		var valid = true;

		final var parties = Optional.ofNullable(query.getParter()).orElse(List.of());
		for (var i = 0; i < parties.size(); i++) {
			final var party = parties.get(i);
			if (party == null || party.isEmpty()) {
				violation(context, ERROR_EMPTY_PARTY)
					.addPropertyNode("parter").addBeanNode().inIterable().atIndex(i)
					.addConstraintViolation();
				valid = false;
			}
		}

		final var processing = query.getBehandling();
		if (processing != null && processing.getPaginering() != null && hasNoSorting(processing)) {
			violation(context, ERROR_PAGINATION_WITHOUT_SORTING)
				.addPropertyNode("behandling").addPropertyNode("paginering")
				.addConstraintViolation();
			valid = false;
		}

		if (isDate(query.getStartDatum()) && isDate(query.getSlutDatum()) && query.getStartDatum().compareTo(query.getSlutDatum()) > 0) {
			violation(context, ERROR_START_AFTER_END)
				.addPropertyNode("startDatum")
				.addConstraintViolation();
			valid = false;
		}
		return valid;
	}

	/**
	 * Replaces the annotation's default message, so each violation points at the field it concerns.
	 */
	private static ConstraintViolationBuilder violation(final ConstraintValidatorContext context, final String message) {
		context.disableDefaultConstraintViolation();
		return context.buildConstraintViolationWithTemplate(message);
	}

	private static boolean hasNoSorting(final Behandling processing) {
		return processing.getSortering() == null || processing.getSortering().isEmpty();
	}

	private static boolean isDate(final String value) {
		return value != null && DATE.matcher(value).matches();
	}
}
