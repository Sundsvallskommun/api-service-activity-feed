package se.sundsvall.minaarenden.integration.db;

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
import se.sundsvall.minaarenden.api.model.Arende;
import se.sundsvall.minaarenden.api.model.Kund;
import se.sundsvall.minaarenden.api.model.Sortering;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity.UNIK_KUNDHANDELSE_ID;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.forPart;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.sorteradEfter;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withKundhandelseTyper;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withMunicipalityId;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withNagonAvTaggarna;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withTidpunktFrom;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withTidpunktTom;

/**
 * Repository- och specifikationstester mot MariaDB (Testcontainers) med schemat från Flyway.
 *
 * @see src/test/resources/db/scripts/testdata-junit.sql för testdata.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
class KundhandelseRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final Kund KUND = Kund.create().withIdentifierare("199009090000").withTyp("Personnummer");
	private static final Kund ANNAN_KUND = Kund.create().withIdentifierare("199001012385").withTyp("Personnummer");

	@Autowired
	private KundhandelseRepository repository;

	@Test
	void findByMunicipalityIdAndKundhandelseId() {
		assertThat(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-BYGG-2")).hasValueSatisfying(entity -> {
			assertThat(entity.getRubrik()).isEqualTo("beslut fattat");
			assertThat(entity.getTidpunkt()).isEqualTo(Instant.parse("2026-09-02T12:30:00Z"));
			assertThat(entity.getTaggar()).containsExactlyInAnyOrder("bygglov", "kundatgard");
			assertThat(entity.isProducentarendetKlart()).isTrue();
		});
		assertThat(repository.findByMunicipalityIdAndKundhandelseId("2262", "TESTKOP-BYGG-2")).isEmpty();
	}

	@Test
	void countByMunicipalityId() {
		assertThat(repository.countByMunicipalityId(MUNICIPALITY_ID)).isEqualTo(4);
		assertThat(repository.countByMunicipalityId("2262")).isEqualTo(1);
		assertThat(repository.countByMunicipalityId("1480")).isZero();
	}

	@Test
	void sparaSatterSkapadOchAndrad() {
		final var entity = repository.saveAndFlush(KundhandelseEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withKundhandelseId("TESTKOP-NY-1")
			.withProducent("Testköpings kommun")
			.withArendeIdentifierare("NY-1")
			.withArendeTyp("Diarienummer")
			.withRubrik("Ny")
			.withBeskrivning("Ny händelse")
			.withSprak("sv")
			.withTidpunkt(Instant.parse("2026-09-20T08:00:00Z"))
			.withKundhandelseTyp("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1")
			.withTaggar(Set.of("ny")));

		assertThat(entity.getId()).isNotNull();
		assertThat(entity.getSkapad()).isNotNull();
		assertThat(entity.getAndrad()).isNull();

		entity.setRubrik("Ändrad");
		final var andrad = repository.saveAndFlush(entity);
		assertThat(andrad.getAndrad()).isNotNull();
	}

	@Test
	void sammaKundhandelseIdTillatsIOlikaKommunerMenInteISamma() {
		final var kopia = KundhandelseEntity.create()
			.withMunicipalityId("2262")
			.withKundhandelseId("TESTKOP-BYGG-1")
			.withProducent("Timrå kommun")
			.withRubrik("r")
			.withBeskrivning("b")
			.withSprak("sv")
			.withTidpunkt(Instant.now())
			.withKundhandelseTyp("TIMRA.TEST.SKAPAD")
			.withVersion("6.1");
		repository.saveAndFlush(kopia);

		final var dubblett = KundhandelseEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withKundhandelseId("TESTKOP-BYGG-1")
			.withProducent("Testköpings kommun")
			.withRubrik("r")
			.withBeskrivning("b")
			.withSprak("sv")
			.withTidpunkt(Instant.now())
			.withKundhandelseTyp("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1");
		// KundhandelseService känner igen krocken på villkorets namn och svarar då 409.
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> repository.saveAndFlush(dubblett))
			.havingCause()
			.isInstanceOfSatisfying(ConstraintViolationException.class, fel -> assertThat(fel.getConstraintName()).isEqualToIgnoringCase(UNIK_KUNDHANDELSE_ID));
	}

	@Test
	void kundhandelseIdJamforsExakt() {
		repository.saveAndFlush(ny("testkop-bygg-1").withRubrik("Gemener"));

		assertThat(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "testkop-bygg-1")).hasValueSatisfying(entity -> assertThat(entity.getRubrik()).isEqualTo("Gemener"));
		assertThat(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-BYGG-1")).hasValueSatisfying(entity -> assertThat(entity.getRubrik()).isNotEqualTo("Gemener"));
	}

	@Test
	void taggarJamforsExakt() {
		repository.saveAndFlush(ny("TESTKOP-TAGG-1").withTaggar(Set.of("bygglov", "Bygglov")));

		final var traffar = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), withNagonAvTaggarna(List.of("Bygglov"))));

		assertThat(traffar).extracting(KundhandelseEntity::getKundhandelseId).containsExactly("TESTKOP-TAGG-1");
	}

	@Test
	void langBeskrivningMedSvenskaTeckenSparas() {
		// 65 535 tecken som API:t tillåter, men 131 070 byte i UTF-8.
		final var beskrivning = "å".repeat(65_535);

		repository.saveAndFlush(ny("TESTKOP-LANG-1").withBeskrivning(beskrivning));

		final var traffar = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(ANNAN_KUND, null)));
		assertThat(traffar).extracting(KundhandelseEntity::getBeskrivning).containsExactly(beskrivning);
	}

	@Test
	void rubrikSorterasEnligtSvenskaRegler() {
		repository.saveAll(List.of(ny("TESTKOP-SORT-1").withRubrik("Ärende"), ny("TESTKOP-SORT-2").withRubrik("Zebra"), ny("TESTKOP-SORT-3").withRubrik("Apa"),
			ny("TESTKOP-SORT-4").withRubrik("Åtgärd")));
		repository.flush();

		final var traffar = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(ANNAN_KUND, null),
			sorteradEfter(List.of(Sortering.create().withAttribut("RUBRIK").withStigande(true)))));

		assertThat(traffar).extracting(KundhandelseEntity::getRubrik).containsExactly("Apa", "Zebra", "Åtgärd", "Ärende");
	}

	@Test
	void kundGerAllaKundensHandelserIKommunenNyastForst() {
		final var traffar = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(KUND, null), sorteradEfter(null)));

		assertThat(traffar).extracting(KundhandelseEntity::getKundhandelseId).containsExactly("TESTKOP-FSK-1", "TESTKOP-BYGG-2", "TESTKOP-BYGG-1");
	}

	@Test
	void kundOchArendeSmalnarAv() {
		final var arende = Arende.create().withIdentifierare("BYGG-1").withTyp("Diarienummer");

		final var traffar = repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(KUND, arende)));

		assertThat(traffar).extracting(KundhandelseEntity::getKundhandelseId).containsExactlyInAnyOrder("TESTKOP-BYGG-1", "TESTKOP-BYGG-2");
	}

	@Test
	void kundMedTillaggMatcharBaraTillagget() {
		final var kund = Kund.create().withIdentifierare("165560001234").withTyp("Organisationsnummer").withTillagg("2");

		assertThat(repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(kund, null)))).isEmpty();
	}

	@Test
	void kundhandelseTypMatcharExaktTypOchPrefix() {
		assertThat(sok(withKundhandelseTyper(List.of("TESTKOP.BYGGLOV")))).containsExactlyInAnyOrder("TESTKOP-BYGG-1", "TESTKOP-BYGG-2");
		assertThat(sok(withKundhandelseTyper(List.of("TESTKOP.BYGGLOV.BESLUT")))).containsExactly("TESTKOP-BYGG-2");
		assertThat(sok(withKundhandelseTyper(List.of("TESTKOP")))).hasSize(3);
		assertThat(sok(withKundhandelseTyper(List.of("TESTKOP.BYGG")))).isEmpty();
		// Understreck är ett vanligt tecken, inte jokertecken i LIKE.
		assertThat(sok(withKundhandelseTyper(List.of("TESTKOP.BYGGLOV.ANSOKAN_MOTTAGE_")))).isEmpty();
		assertThat(sok(withKundhandelseTyper(List.of()))).hasSize(3);
	}

	@Test
	void nagonAvTaggarnaUtanDubbletter() {
		assertThat(sok(withNagonAvTaggarna(List.of("kundatgard", "bygglov")))).containsExactlyInAnyOrder("TESTKOP-BYGG-1", "TESTKOP-BYGG-2", "TESTKOP-FSK-1");
		assertThat(sok(withNagonAvTaggarna(List.of("forskola")))).containsExactly("TESTKOP-FSK-1");
		assertThat(sok(withNagonAvTaggarna(null))).hasSize(3);
	}

	@Test
	void tidpunktsintervall() {
		assertThat(sok(Specification.allOf(withTidpunktFrom(Instant.parse("2026-09-01T00:00:00Z")), withTidpunktTom(Instant.parse("2026-09-05T00:00:00Z")))))
			.containsExactly("TESTKOP-BYGG-2");
		assertThat(sok(Specification.allOf(withTidpunktFrom(null), withTidpunktTom(null)))).hasSize(3);
	}

	@Test
	void sorteringPaFleraAttributOchPaginering() {
		final var sortering = List.of(
			Sortering.create().withAttribut("PRODUCENTARENDETKRAVERKUNDATGARD").withStigande(false),
			Sortering.create().withAttribut("RUBRIK").withStigande(true));
		final var specification = Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(KUND, null), sorteradEfter(sortering));

		final var sida = repository.findAll(specification, new OffsetPageable(1, 1));

		// Kräver kundåtgärd först (beslut fattat, Plats erbjuden), sedan rubrik skiftlägesokänsligt.
		assertThat(sida.getTotalElements()).isEqualTo(3);
		assertThat(sida.getContent()).extracting(KundhandelseEntity::getKundhandelseId).containsExactly("TESTKOP-FSK-1");
	}

	@Test
	void allaSorteringsattribut() {
		for (final var attribut : List.of("RUBRIK", "BESKRIVNING", "PRODUCENT", "TIDPUNKT", "KUNDHANDELSETYP", "PRODUCENTARENDETKRAVERKUNDATGARD", "PRODUCENTARENDETKLART")) {
			final var sortering = List.of(Sortering.create().withAttribut(attribut).withStigande(true));
			assertThat(repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), sorteradEfter(sortering)))).as(attribut).hasSize(4);
		}
	}

	private List<String> sok(final Specification<KundhandelseEntity> specification) {
		return repository.findAll(Specification.allOf(withMunicipalityId(MUNICIPALITY_ID), forPart(KUND, null), specification)).stream()
			.map(KundhandelseEntity::getKundhandelseId)
			.toList();
	}

	/**
	 * En kundhändelse för en kund som inte finns i testdatat.
	 */
	private static KundhandelseEntity ny(final String kundhandelseId) {
		return KundhandelseEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withKundhandelseId(kundhandelseId)
			.withProducent("Testköpings kommun")
			.withKundIdentifierare(ANNAN_KUND.getIdentifierare())
			.withKundTyp(ANNAN_KUND.getTyp())
			.withRubrik("r")
			.withBeskrivning("b")
			.withSprak("sv")
			.withTidpunkt(Instant.parse("2026-09-20T08:00:00Z"))
			.withKundhandelseTyp("TESTKOP.TEST.SKAPAD")
			.withVersion("6.1");
	}
}
