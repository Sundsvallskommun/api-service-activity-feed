package se.sundsvall.activityfeed.service.mapper;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.CachadKundhandelse;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.Kundhandelse;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.api.model.Part;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.activityfeed.util.DateTimeUtil;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.function.Predicate.not;

/**
 * Maps between the API model and the activity cache. utokadInformation and referenser are stored as JSON text.
 */
public final class ActivityMapper {

	static final String DEFAULT_LANGUAGE = "sv";

	private static final JsonMapper JSON_MAPPER = JsonMapper.shared();

	private ActivityMapper() {}

	public static Kundhandelse toKundhandelse(final ActivityEntity entity) {
		return Kundhandelse.create()
			.withKundhandelseId(entity.getActivityId())
			.withRubrik(entity.getTitle())
			.withBeskrivning(entity.getDescription())
			.withSprak(entity.getLanguage())
			.withProducent(entity.getProducer())
			.withTidpunkt(DateTimeUtil.format(entity.getOccurredAt()))
			.withKundhandelseTyp(entity.getActivityType())
			.withProducentarendetKraverKundatgard(entity.isCustomerActionRequired())
			.withProducentarendetKlart(entity.isErrandCompleted())
			.withVersion(entity.getVersion())
			.withUtokadInformation(readJson(entity.getExtendedInformation()))
			.withReferenser(readJson(entity.getReferenceData()));
	}

	public static Part toPart(final ActivityEntity entity) {
		final var customer = Optional.ofNullable(entity.getCustomerIdentifier())
			.map(identifier -> Kund.create().withIdentifierare(identifier).withTyp(entity.getCustomerType()).withTillagg(entity.getCustomerSuffix()))
			.orElse(null);
		final var errand = Optional.ofNullable(entity.getErrandIdentifier())
			.map(identifier -> Arende.create().withIdentifierare(identifier).withTyp(entity.getErrandType()))
			.orElse(null);
		return Part.create().withKund(customer).withArende(errand);
	}

	public static CachadKundhandelse toCachadKundhandelse(final ActivityEntity entity) {
		return CachadKundhandelse.create()
			.withPart(toPart(entity))
			.withKundhandelse(toKundhandelse(entity))
			.withTaggar(List.copyOf(entity.getTags()));
	}

	/**
	 * Fills a new or existing entity from an ingested activity. producer, language and version come from the
	 * configuration when the activity leaves them out.
	 *
	 * @param  municipalityId  municipality
	 * @param  producer        the producer for the municipality
	 * @param  standardVersion version of the standard to use when the activity does not give one
	 * @param  activity        ingested activity
	 * @param  entity          entity to fill
	 * @return                 the entity
	 */
	public static ActivityEntity toEntity(final String municipalityId, final Producer producer, final String standardVersion, final NyKundhandelse activity,
		final ActivityEntity entity) {

		final var customer = Optional.ofNullable(activity.getPart().getKund());
		final var errand = Optional.ofNullable(activity.getPart().getArende());
		return entity
			.withMunicipalityId(municipalityId)
			.withActivityId(activity.getKundhandelseId())
			.withProducer(producer.name())
			.withCustomerIdentifier(customer.map(Kund::getIdentifierare).orElse(null))
			.withCustomerType(customer.map(Kund::getTyp).orElse(null))
			.withCustomerSuffix(customer.map(Kund::getTillagg).map(ActivityMapper::blankToNull).orElse(null))
			.withErrandIdentifier(errand.map(Arende::getIdentifierare).orElse(null))
			.withErrandType(errand.map(Arende::getTyp).orElse(null))
			.withTitle(activity.getRubrik())
			.withDescription(activity.getBeskrivning())
			.withLanguage(Optional.ofNullable(blankToNull(activity.getSprak())).orElse(DEFAULT_LANGUAGE))
			.withOccurredAt(occurredAt(entity.getOccurredAt(), activity.getTidpunkt()))
			.withActivityType(activity.getKundhandelseTyp())
			.withCustomerActionRequired(activity.getProducentarendetKraverKundatgard())
			.withErrandCompleted(activity.getProducentarendetKlart())
			.withVersion(Optional.ofNullable(blankToNull(activity.getVersion())).orElse(standardVersion))
			.withExtendedInformation(writeJson(activity.getUtokadInformation()))
			.withReferenceData(writeJson(activity.getReferenser()))
			.withTags(Optional.ofNullable(activity.getTaggar()).map(LinkedHashSet::new).orElse(null));
	}

	static JsonNode readJson(final String json) {
		if (json == null) {
			return null;
		}
		try {
			return JSON_MAPPER.readTree(json);
		} catch (final JacksonException e) {
			throw new IllegalStateException("Invalid JSON in the database", e);
		}
	}

	static String writeJson(final JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		return JSON_MAPPER.writeValueAsString(node);
	}

	private static String blankToNull(final String value) {
		return Optional.ofNullable(value).filter(not(String::isBlank)).orElse(null);
	}

	/**
	 * Truncates the incoming value to milliseconds, which is what occurred_at holds, and keeps the stored value when the
	 * point in time is unchanged. Hibernate compares OffsetDateTime with equals, so a different offset or nanoseconds the
	 * database drops would otherwise count as a change and update the row.
	 */
	private static OffsetDateTime occurredAt(final OffsetDateTime current, final OffsetDateTime incoming) {
		final var truncated = incoming.truncatedTo(MILLIS);
		return current != null && current.isEqual(truncated) ? current : truncated;
	}
}
