package se.sundsvall.activityfeed.integration.db;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ActivityEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusSeconds(ThreadLocalRandom.current().nextLong(1, 4_000_000_000L)), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ActivityEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("tags"),
			hasValidBeanEqualsExcluding("tags"),
			hasValidBeanToStringExcluding("tags")));
	}

	@Test
	void testBuilderMethods() {
		final var timestamp = OffsetDateTime.parse("2026-09-20T08:00:00Z");
		final var entity = ActivityEntity.create()
			.withId("5d8c3e0a-1b7f-4c2e-9a6d-3f1e2b4c5a6d")
			.withMunicipalityId("2281")
			.withActivityId("ID")
			.withProducer("Producent")
			.withCustomerIdentifier("199009090000")
			.withCustomerType("Personnummer")
			.withCustomerSuffix("1")
			.withErrandIdentifier("A-1")
			.withErrandType("Diarienummer")
			.withTitle("Rubrik")
			.withDescription("Beskrivning")
			.withLanguage("sv")
			.withOccurredAt(timestamp)
			.withActivityType("P.A.B")
			.withCustomerActionRequired(true)
			.withErrandCompleted(true)
			.withVersion("6.1")
			.withExtendedInformation("{}")
			.withReferenceData("{}")
			.withCreated(timestamp)
			.withModified(timestamp)
			.withTags(Set.of("tagg"));

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.isErrandCompleted()).isTrue();
		assertThat(entity.isCustomerActionRequired()).isTrue();
		assertThat(entity.withTags(null).getTags()).isEmpty();
	}

	@Test
	void personalIdentityNumberIsMaskedInToString() {
		assertThat(ActivityEntity.create().withCustomerIdentifier("199009090000").toString()).doesNotContain("199009090000");
	}

	@Test
	void createdAndModifiedAreSetByLifecycle() throws Exception {
		final var entity = ActivityEntity.create();
		final var prePersist = ActivityEntity.class.getDeclaredMethod("prePersist");
		final var preUpdate = ActivityEntity.class.getDeclaredMethod("preUpdate");
		prePersist.setAccessible(true);
		preUpdate.setAccessible(true);

		prePersist.invoke(entity);
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getModified()).isNull();

		preUpdate.invoke(entity);
		assertThat(entity.getModified()).isNotNull();
	}
}
