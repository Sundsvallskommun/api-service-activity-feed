package se.sundsvall.activityfeed.integration.db.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.Sortering;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity_;

import static org.springframework.data.jpa.domain.Specification.unrestricted;

/**
 * Database filters and sorting that correspond to the fields in fraga.
 */
public final class ActivitySpecification {

	private ActivitySpecification() {}

	public static Specification<ActivityEntity> withMunicipalityId(final String municipalityId) {
		return (root, _, cb) -> cb.equal(root.get(ActivityEntity_.municipalityId), municipalityId);
	}

	/**
	 * The party matches if both the given customer and the given errand match (spec: "svaret uppfyller båda
	 * kriterierna").
	 */
	public static Specification<ActivityEntity> forParty(final Kund customer, final Arende errand) {
		return (root, _, cb) -> {
			final List<Predicate> predicates = new ArrayList<>();
			if (customer != null) {
				predicates.add(cb.equal(root.get(ActivityEntity_.customerIdentifier), customer.getIdentifierare()));
				predicates.add(cb.equal(root.get(ActivityEntity_.customerType), customer.getTyp()));
				if (customer.getTillagg() != null && !customer.getTillagg().isBlank()) {
					predicates.add(cb.equal(root.get(ActivityEntity_.customerSuffix), customer.getTillagg()));
				}
			}
			if (errand != null) {
				predicates.add(cb.equal(root.get(ActivityEntity_.errandIdentifier), errand.getIdentifierare()));
				predicates.add(cb.equal(root.get(ActivityEntity_.errandType), errand.getTyp()));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	/**
	 * An activity type matches the filter if it equals the filter or sits below it (prefix followed by a dot).
	 */
	public static Specification<ActivityEntity> withActivityTypes(final List<String> types) {
		if (types == null || types.isEmpty()) {
			return unrestricted();
		}
		return (root, _, cb) -> cb.or(types.stream()
			.map(type -> cb.or(
				cb.equal(root.get(ActivityEntity_.activityType), type),
				cb.like(root.get(ActivityEntity_.activityType), escapeLike(type) + ".%", '!')))
			.toArray(Predicate[]::new));
	}

	/**
	 * At least one of the tags must be on the activity. Expressed as EXISTS instead of JOIN + DISTINCT, so that sorting
	 * on expressions (e.g. LOWER(title)) works and no duplicates appear.
	 */
	public static Specification<ActivityEntity> withAnyTag(final List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return unrestricted();
		}
		return (root, query, cb) -> {
			final var subquery = query.subquery(String.class);
			final var inner = subquery.from(ActivityEntity.class);
			final var tag = inner.join(ActivityEntity_.tags);
			subquery.select(inner.get(ActivityEntity_.id)).where(cb.equal(inner.get(ActivityEntity_.id), root.get(ActivityEntity_.id)), tag.in(tags));
			return cb.exists(subquery);
		};
	}

	public static Specification<ActivityEntity> withOccurredFrom(final OffsetDateTime from) {
		return from == null ? unrestricted() : (root, _, cb) -> cb.greaterThanOrEqualTo(root.get(ActivityEntity_.occurredAt), from);
	}

	public static Specification<ActivityEntity> withOccurredTo(final OffsetDateTime to) {
		return to == null ? unrestricted() : (root, _, cb) -> cb.lessThanOrEqualTo(root.get(ActivityEntity_.occurredAt), to);
	}

	/**
	 * Sorting according to fraga.behandling.sortering, done in the database. Without a given sorting, newest first.
	 * TIDPUNKT sorts on the stored DATETIME column, not on the RFC 3339 text, so the order is chronological even when
	 * the UTC offset differs (e.g. at the switch to winter time). The primary key goes last as a tie-breaker, so
	 * pagination is stable when values are equal. The COUNT query skips the sorting.
	 */
	public static Specification<ActivityEntity> sortedBy(final List<Sortering> sorting) {
		return (root, query, cb) -> {
			if (query == null || Long.class.equals(query.getResultType())) {
				return null;
			}
			final List<Order> orders = new ArrayList<>();
			if (sorting == null || sorting.isEmpty()) {
				orders.add(cb.desc(root.get(ActivityEntity_.occurredAt)));
				orders.add(cb.desc(root.get(ActivityEntity_.id)));
			} else {
				for (final var s : sorting) {
					final var expression = sortExpression(root, cb, s.getAttribut());
					orders.add(Boolean.TRUE.equals(s.getStigande()) ? cb.asc(expression) : cb.desc(expression));
				}
				orders.add(cb.asc(root.get(ActivityEntity_.id)));
			}
			query.orderBy(orders);
			return null;
		};
	}

	static Expression<?> sortExpression(final Root<ActivityEntity> root, final CriteriaBuilder cb, final String attribute) {
		return switch (SortAttribute.valueOf(attribute)) {
			case RUBRIK -> cb.lower(root.get(ActivityEntity_.title));
			case BESKRIVNING -> cb.lower(root.get(ActivityEntity_.description));
			case PRODUCENT -> cb.lower(root.get(ActivityEntity_.producer));
			case TIDPUNKT -> root.get(ActivityEntity_.occurredAt);
			case KUNDHANDELSETYP -> root.get(ActivityEntity_.activityType);
			case PRODUCENTARENDETKRAVERKUNDATGARD -> root.get(ActivityEntity_.customerActionRequired);
			case PRODUCENTARENDETKLART -> root.get(ActivityEntity_.errandCompleted);
		};
	}

	/**
	 * The values of sortering.attribut in the API, which Sortering has already validated. A new value does not compile
	 * until it has a sort expression.
	 */
	enum SortAttribute {
		RUBRIK, BESKRIVNING, PRODUCENT, TIDPUNKT, KUNDHANDELSETYP, PRODUCENTARENDETKRAVERKUNDATGARD, PRODUCENTARENDETKLART
	}

	static String escapeLike(final String value) {
		return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}
}
