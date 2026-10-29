package se.sundsvall.minaarenden.integration.db;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class KundhandelseEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> Instant.ofEpochSecond(ThreadLocalRandom.current().nextLong(1, 4_000_000_000L)), Instant.class);
	}

	@Test
	void testBean() {
		assertThat(KundhandelseEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMetoder() {
		final var tidpunkt = Instant.parse("2026-09-20T08:00:00Z");
		final var entity = KundhandelseEntity.create()
			.withId("5d8c3e0a-1b7f-4c2e-9a6d-3f1e2b4c5a6d")
			.withMunicipalityId("2281")
			.withKundhandelseId("ID")
			.withProducent("Producent")
			.withKundIdentifierare("199009090000")
			.withKundTyp("Personnummer")
			.withKundTillagg("1")
			.withArendeIdentifierare("A-1")
			.withArendeTyp("Diarienummer")
			.withRubrik("Rubrik")
			.withBeskrivning("Beskrivning")
			.withSprak("sv")
			.withTidpunkt(tidpunkt)
			.withKundhandelseTyp("P.A.B")
			.withProducentarendetKraverKundatgard(true)
			.withProducentarendetKlart(true)
			.withVersion("6.1")
			.withUtokadInformation("{}")
			.withReferenser("{}")
			.withSkapad(tidpunkt)
			.withAndrad(tidpunkt)
			.withTaggar(Set.of("tagg"));

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.isProducentarendetKlart()).isTrue();
		assertThat(entity.isProducentarendetKraverKundatgard()).isTrue();
		assertThat(entity.withTaggar(null).getTaggar()).isEmpty();
	}

	@Test
	void personnummerMaskerasIToString() {
		assertThat(KundhandelseEntity.create().withKundIdentifierare("199009090000").toString()).doesNotContain("199009090000");
	}

	@Test
	void skapadOchAndradSattsAvLivscykeln() throws Exception {
		final var entity = KundhandelseEntity.create();
		final var prePersist = KundhandelseEntity.class.getDeclaredMethod("prePersist");
		final var preUpdate = KundhandelseEntity.class.getDeclaredMethod("preUpdate");
		prePersist.setAccessible(true);
		preUpdate.setAccessible(true);

		prePersist.invoke(entity);
		assertThat(entity.getSkapad()).isNotNull();
		assertThat(entity.getAndrad()).isNull();

		preUpdate.invoke(entity);
		assertThat(entity.getAndrad()).isNotNull();
	}
}
