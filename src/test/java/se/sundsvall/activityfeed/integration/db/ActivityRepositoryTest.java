package se.sundsvall.activityfeed.integration.db;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.Sortering;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.activityfeed.service.mapper.ActivityMapper;
import se.sundsvall.activityfeed.util.DateTimeUtil;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import static se.sundsvall.activityfeed.integration.db.model.ActivityEntity.UNIQUE_ACTIVITY_ID;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.forParty;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.sortedBy;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withActivityTypes;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withAnyTag;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withMunicipalityId;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withOccurredFrom;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withOccurredTo;
import static se.sundsvall.activityfeed.service.TestdataFactory.PRODUCER;
import static se.sundsvall.activityfeed.service.TestdataFactory.nyKundhandelse;

/**
 * Repository and specification tests against MariaDB (Testcontainers) with the schema from Flyway.
 *
 * @see src/test/resources/db/scripts/testdata-junit.sql for test data.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class ActivityRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final Kund CUSTOMER = Kund.create().withIdentifierare("199009090000").withTyp("Personnummer");
	private static final Kund OTHER_CUSTOMER = Kund.create().withIdentifierare("199001012385").withTyp("Personnummer");

	@Autowired
	private ActivityRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findByMunicipalityIdAndActivityId() {
		assertThat(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-BYGG-2")).hasValueSatisfying(entity -> {
			assertThat(entity.getTitle()).isEqualTo("beslut fattat");
			assertThat(entity.getOccurredAt()).isAtSameInstantAs(OffsetDateTime.parse("2026-09-02T12:30:00Z"));
			assertThat(entity.getTags()).containsExactlyInAnyOrder("bygglov", "kundatgard");
			assertThat(entity.isErrandCompleted()).isTrue();
		});
		assertThat(repository.findByMunicipalityIdAndActivityId("2262", "TESTKOP-BYGG-2")).isEmpty();
	}

	/**
	 * Outside a transaction the entity is detached when the repository call returns, with the tags not loaded.
	 */
	@Test
	@Transactional(propagation = NOT_SUPPORTED)
	void detachedEntityWithUnloadedTagsCanBeLoggedAndCompared() {
		final var entity = repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-BYGG-2").orElseThrow();
		final var sameRow = repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-BYGG-2").orElseThrow();

		assertThat(Hibernate.isInitialized(entity.getTags())).isFalse();
		assertThatNoException().isThrownBy(entity::toString);
		assertThatNoException().isThrownBy(entity::hashCode);
		assertThat(entity).isEqualTo(sameRow);
	}

	@Test
	void countByMunicipalityId() {
		assertThat(repository.countByMunicipalityId(MUNICIPALITY_ID)).isEqualTo(4);
		assertThat(repository.countByMunicipalityId("2262")).isEqualTo(1);
		assertThat(repository.countByMunicipalityId("1480")).isZero();
	}

	@Test
	void saveSetsCreatedAndModified() {
		final var entity = repository.saveAndFlush(ActivityEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withActivityId("TESTKOP-NY-1")
			.withProducer("Testköpings kommun")
			.withErrandIdentifier("NY-1")
			.withErrandType("Diarienummer")
			.withTitle("Ny")
			.withDescription("Ny händelse")
			.withLanguage("sv")
			.withOccurredAt(OffsetDateTime.parse("2026-09-20T08:00:00Z"))
			.withActivityType("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1")
			.withTags(Set.of("ny")));

		assertThat(entity.getId()).isNotNull();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getModified()).isNull();

		entity.setTitle("Ändrad");
		final var modified = repository.saveAndFlush(entity);
		assertThat(modified.getModified()).isNotNull();
	}

	/**
	 * A resend of an unchanged activity writes nothing. +05:30 is neither UTC nor Swedish time, so the offset read back
	 * from the database differs from the one sent in, wherever the tests run. The nanoseconds do not fit in occurred_at,
	 * which holds milliseconds. Only the point in time to the millisecond is the same.
	 */
	@Test
	void resendOfUnchangedActivityDoesNotUpdateIt() {
		final var activity = nyKundhandelse("TESTKOP-OMSANDNING-1").withTidpunkt(OffsetDateTime.parse("2026-09-20T13:30:00.123456789+05:30"));
		repository.saveAndFlush(ActivityMapper.toEntity(MUNICIPALITY_ID, PRODUCER, "6.1", activity, ActivityEntity.create()));
		entityManager.clear();

		final var stored = repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-OMSANDNING-1").orElseThrow();
		repository.saveAndFlush(ActivityMapper.toEntity(MUNICIPALITY_ID, PRODUCER, "6.1", activity, stored));
		entityManager.clear();

		assertThat(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-OMSANDNING-1"))
			.hasValueSatisfying(entity -> assertThat(entity.getModified()).isNull());
	}

	@Test
	void sameActivityIdAllowedInDifferentMunicipalitiesButNotInSame() {
		final var copy = ActivityEntity.create()
			.withMunicipalityId("2262")
			.withActivityId("TESTKOP-BYGG-1")
			.withProducer("Timrå kommun")
			.withTitle("r")
			.withDescription("b")
			.withLanguage("sv")
			.withOccurredAt(OffsetDateTime.now())
			.withActivityType("TIMRA.TEST.SKAPAD")
			.withVersion("6.1");
		repository.saveAndFlush(copy);

		final var duplicate = ActivityEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withActivityId("TESTKOP-BYGG-1")
			.withProducer("Testköpings kommun")
			.withTitle("r")
			.withDescription("b")
			.withLanguage("sv")
			.withOccurredAt(OffsetDateTime.now())
			.withActivityType("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1");
		// ActivityService recognizes the collision by the constraint name and answers 409.
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> repository.saveAndFlush(duplicate))
			.havingCause()
			.isInstanceOfSatisfying(ConstraintViolationException.class, error -> assertThat(error.getConstraintName()).isEqualToIgnoringCase(UNIQUE_ACTIVITY_ID));
	}

	@Test
	void activityIdIsComparedExactly() {
		repository.saveAndFlush(activity("testkop-bygg-1").withTitle("Gemener"));

		assertThat(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "testkop-bygg-1")).hasValueSatisfying(entity -> assertThat(entity.getTitle()).isEqualTo("Gemener"));
		assertThat(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-BYGG-1")).hasValueSatisfying(entity -> assertThat(entity.getTitle()).isNotEqualTo("Gemener"));
	}

	@Test
	void tagsAreComparedExactly() {
		repository.saveAndFlush(activity("TESTKOP-TAGG-1").withTags(Set.of("bygglov", "Bygglov")));

		final var hits = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), withAnyTag(List.of("Bygglov"))));

		assertThat(hits).extracting(ActivityEntity::getActivityId).containsExactly("TESTKOP-TAGG-1");
	}

	@Test
	void longDescriptionWithSwedishCharactersIsSaved() {
		// 65 535 characters, which the API allows, but 131 070 bytes in UTF-8.
		final var description = "å".repeat(65_535);

		repository.saveAndFlush(activity("TESTKOP-LANG-1").withDescription(description));

		final var hits = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(OTHER_CUSTOMER, null)));
		assertThat(hits).extracting(ActivityEntity::getDescription).containsExactly(description);
	}

	@Test
	void titleIsSortedBySwedishRules() {
		repository.saveAll(List.of(activity("TESTKOP-SORT-1").withTitle("Ärende"), activity("TESTKOP-SORT-2").withTitle("Zebra"), activity("TESTKOP-SORT-3").withTitle("Apa"),
			activity("TESTKOP-SORT-4").withTitle("Åtgärd")));
		repository.flush();

		final var hits = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(OTHER_CUSTOMER, null),
			sortedBy(List.of(Sortering.create().withAttribut("RUBRIK").withStigande(true)))));

		assertThat(hits).extracting(ActivityEntity::getTitle).containsExactly("Apa", "Zebra", "Åtgärd", "Ärende");
	}

	@Test
	void customerGivesAllCustomerActivitiesInMunicipalityNewestFirst() {
		final var hits = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(CUSTOMER, null), sortedBy(null)));

		assertThat(hits).extracting(ActivityEntity::getActivityId).containsExactly("TESTKOP-FSK-1", "TESTKOP-BYGG-2", "TESTKOP-BYGG-1");
	}

	@Test
	void customerAndErrandNarrowDown() {
		final var errand = Arende.create().withIdentifierare("BYGG-1").withTyp("Diarienummer");

		final var hits = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(CUSTOMER, errand)));

		assertThat(hits).extracting(ActivityEntity::getActivityId).containsExactlyInAnyOrder("TESTKOP-BYGG-1", "TESTKOP-BYGG-2");
	}

	@Test
	void customerWithSuffixMatchesOnlyTheSuffix() {
		final var customer = Kund.create().withIdentifierare("165560001234").withTyp("Organisationsnummer").withTillagg("2");

		assertThat(repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(customer, null)))).isEmpty();
	}

	@Test
	void activityTypeMatchesExactTypeAndPrefix() {
		assertThat(search(withActivityTypes(List.of("TESTKOP.BYGGLOV")))).containsExactlyInAnyOrder("TESTKOP-BYGG-1", "TESTKOP-BYGG-2");
		assertThat(search(withActivityTypes(List.of("TESTKOP.BYGGLOV.BESLUT")))).containsExactly("TESTKOP-BYGG-2");
		assertThat(search(withActivityTypes(List.of("TESTKOP")))).hasSize(3);
		assertThat(search(withActivityTypes(List.of("TESTKOP.BYGG")))).isEmpty();
		// Underscore is a plain character, not a wildcard in LIKE.
		assertThat(search(withActivityTypes(List.of("TESTKOP.BYGGLOV.ANSOKAN_MOTTAGE_")))).isEmpty();
		assertThat(search(withActivityTypes(List.of()))).hasSize(3);
	}

	@Test
	void anyTagWithoutDuplicates() {
		assertThat(search(withAnyTag(List.of("kundatgard", "bygglov")))).containsExactlyInAnyOrder("TESTKOP-BYGG-1", "TESTKOP-BYGG-2", "TESTKOP-FSK-1");
		assertThat(search(withAnyTag(List.of("forskola")))).containsExactly("TESTKOP-FSK-1");
		assertThat(search(withAnyTag(null))).hasSize(3);
	}

	@Test
	void timeRange() {
		assertThat(search(Specification.allOf(withOccurredFrom(OffsetDateTime.parse("2026-09-01T00:00:00Z")), withOccurredTo(OffsetDateTime.parse("2026-09-05T00:00:00Z")))))
			.containsExactly("TESTKOP-BYGG-2");
		assertThat(search(Specification.allOf(withOccurredFrom(null), withOccurredTo(null)))).hasSize(3);
	}

	/**
	 * 02:30 on 2026-10-25 happens twice in Swedish time, first in summer time and an hour later in winter time. The day is
	 * 25 hours long, and both ends of a date filter are inclusive to the millisecond (occurred_at is DATETIME(3)).
	 */
	@Test
	void switchToWinterTimeIsSortedAndFilteredInSwedishTime() {
		repository.saveAll(List.of(
			activity("TESTKOP-DST-SISTA").withOccurredAt(OffsetDateTime.parse("2026-10-25T23:59:59.999+01:00")),
			activity("TESTKOP-DST-VINTER").withOccurredAt(OffsetDateTime.parse("2026-10-25T02:30:00+01:00")),
			activity("TESTKOP-DST-SOMMAR").withOccurredAt(OffsetDateTime.parse("2026-10-25T02:30:00+02:00")),
			activity("TESTKOP-DST-FORSTA").withOccurredAt(OffsetDateTime.parse("2026-10-25T00:00:00+02:00")),
			activity("TESTKOP-DST-FORE").withOccurredAt(OffsetDateTime.parse("2026-10-24T23:59:59.999+02:00")),
			activity("TESTKOP-DST-EFTER").withOccurredAt(OffsetDateTime.parse("2026-10-26T00:00:00+01:00"))));
		repository.flush();
		entityManager.clear();

		final var hits = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(OTHER_CUSTOMER, null),
			withOccurredFrom(DateTimeUtil.startOfDay("2026-10-25")), withOccurredTo(DateTimeUtil.endOfDay("2026-10-25")),
			sortedBy(List.of(Sortering.create().withAttribut("TIDPUNKT").withStigande(true)))));

		assertThat(hits).extracting(ActivityEntity::getActivityId)
			.containsExactly("TESTKOP-DST-FORSTA", "TESTKOP-DST-SOMMAR", "TESTKOP-DST-VINTER", "TESTKOP-DST-SISTA");
	}

	@Test
	void sortingOnSeveralAttributesAndPagination() {
		final var sorting = List.of(
			Sortering.create().withAttribut("PRODUCENTARENDETKRAVERKUNDATGARD").withStigande(false),
			Sortering.create().withAttribut("RUBRIK").withStigande(true));
		final var specification = Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(CUSTOMER, null), sortedBy(sorting));

		final var page = repository.findAll(specification, new OffsetPageable(1, 1));

		// Customer action required first (beslut fattat, Plats erbjuden), then title case-insensitively.
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent()).extracting(ActivityEntity::getActivityId).containsExactly("TESTKOP-FSK-1");
	}

	/**
	 * Every value the API accepts has a sort expression. The values come from @OneOf on Sortering.attribut, so a value
	 * added there but not to ActivitySpecification.SortAttribute fails here instead of in a query.
	 */
	@Test
	void allSortAttributes() throws NoSuchFieldException {
		final var attributes = Sortering.class.getDeclaredField("attribut").getAnnotation(OneOf.class).value();

		assertThat(attributes).isNotEmpty();
		for (final var attribute : attributes) {
			final var sorting = List.of(Sortering.create().withAttribut(attribute).withStigande(true));
			assertThat(repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), sortedBy(sorting)))).as(attribute).hasSize(4);
		}
	}

	private List<String> search(final Specification<ActivityEntity> specification) {
		return repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forParty(CUSTOMER, null), specification)).stream()
			.map(ActivityEntity::getActivityId)
			.toList();
	}

	/**
	 * An activity for a customer that is not in the test data.
	 */
	private static ActivityEntity activity(final String activityId) {
		return ActivityEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withActivityId(activityId)
			.withProducer("Testköpings kommun")
			.withCustomerIdentifier(OTHER_CUSTOMER.getIdentifierare())
			.withCustomerType(OTHER_CUSTOMER.getTyp())
			.withTitle("r")
			.withDescription("b")
			.withLanguage("sv")
			.withOccurredAt(OffsetDateTime.parse("2026-09-20T08:00:00Z"))
			.withActivityType("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1");
	}
}
