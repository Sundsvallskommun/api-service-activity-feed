package se.sundsvall.activityfeed.integration.db;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.Sortering;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.activityfeed.integration.db.model.ActivityEntity.UNIQUE_ACTIVITY_ID;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.forParty;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.sortedBy;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withActivityTypes;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withAnyTag;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withMunicipalityId;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withOccurredFrom;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withOccurredTo;

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

	@Test
	void findByMunicipalityIdAndActivityId() {
		assertThat(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-BYGG-2")).hasValueSatisfying(entity -> {
			assertThat(entity.getTitle()).isEqualTo("beslut fattat");
			assertThat(entity.getOccurredAt()).isEqualTo(Instant.parse("2026-09-02T12:30:00Z"));
			assertThat(entity.getTags()).containsExactlyInAnyOrder("bygglov", "kundatgard");
			assertThat(entity.isErrandCompleted()).isTrue();
		});
		assertThat(repository.findByMunicipalityIdAndActivityId("2262", "TESTKOP-BYGG-2")).isEmpty();
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
			.withOccurredAt(Instant.parse("2026-09-20T08:00:00Z"))
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

	@Test
	void sameActivityIdAllowedInDifferentMunicipalitiesButNotInSame() {
		final var copy = ActivityEntity.create()
			.withMunicipalityId("2262")
			.withActivityId("TESTKOP-BYGG-1")
			.withProducer("Timrå kommun")
			.withTitle("r")
			.withDescription("b")
			.withLanguage("sv")
			.withOccurredAt(Instant.now())
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
			.withOccurredAt(Instant.now())
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
		assertThat(search(Specification.allOf(withOccurredFrom(Instant.parse("2026-09-01T00:00:00Z")), withOccurredTo(Instant.parse("2026-09-05T00:00:00Z")))))
			.containsExactly("TESTKOP-BYGG-2");
		assertThat(search(Specification.allOf(withOccurredFrom(null), withOccurredTo(null)))).hasSize(3);
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

	@Test
	void allSortAttributes() {
		for (final var attribute : List.of("RUBRIK", "BESKRIVNING", "PRODUCENT", "TIDPUNKT", "KUNDHANDELSETYP", "PRODUCENTARENDETKRAVERKUNDATGARD", "PRODUCENTARENDETKLART")) {
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
			.withOccurredAt(Instant.parse("2026-09-20T08:00:00Z"))
			.withActivityType("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1");
	}
}
