package se.sundsvall.activityfeed.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.Part;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static se.sundsvall.activityfeed.service.TestdataFactory.MUNICIPALITY_ID;
import static se.sundsvall.activityfeed.service.TestdataFactory.PRODUCER;
import static se.sundsvall.activityfeed.service.TestdataFactory.entity;
import static se.sundsvall.activityfeed.service.TestdataFactory.nyKundhandelse;

class ActivityMapperTest {

	@Test
	void toEntityFillsInDefaults() {
		final var activity = nyKundhandelse("TESTKOP-1").withTidpunkt(OffsetDateTime.parse("2026-09-15T12:00:00+02:00"));
		activity.getPart().getKund().withTillagg(" ");

		final var entity = ActivityMapper.toEntity(MUNICIPALITY_ID, PRODUCER, "6.1", activity, ActivityEntity.create());

		assertThat(entity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entity.getActivityId()).isEqualTo("TESTKOP-1");
		assertThat(entity.getProducer()).isEqualTo("Testköpings kommun");
		assertThat(entity.getCustomerIdentifier()).isEqualTo("199009090000");
		assertThat(entity.getCustomerType()).isEqualTo("Personnummer");
		assertThat(entity.getCustomerSuffix()).isNull();
		assertThat(entity.getErrandIdentifier()).isEqualTo("BYGG-1");
		assertThat(entity.getErrandType()).isEqualTo("Diarienummer");
		assertThat(entity.getLanguage()).isEqualTo("sv");
		assertThat(entity.getVersion()).isEqualTo("6.1");
		assertThat(entity.getOccurredAt()).isAtSameInstantAs(OffsetDateTime.parse("2026-09-15T10:00:00Z"));
		assertThat(entity.getReferenceData()).isEqualTo("{\"diarienummer\":\"BYGG-1\"}");
		assertThat(entity.getExtendedInformation()).isNull();
		assertThat(entity.getTags()).containsExactly("bygglov");
	}

	@Test
	void toEntityReplacesPartyAndKeepsGivenValues() {
		final var activity = nyKundhandelse("TESTKOP-1")
			.withPart(Part.create().withKund(Kund.create().withIdentifierare("165560001234").withTyp("Organisationsnummer").withTillagg("2")))
			.withSprak("en")
			.withVersion("6.0")
			.withTaggar(null);

		final var entity = ActivityMapper.toEntity(MUNICIPALITY_ID, PRODUCER, "6.1", activity, entity("TESTKOP-1"));

		assertThat(entity.getCustomerSuffix()).isEqualTo("2");
		assertThat(entity.getErrandIdentifier()).isNull();
		assertThat(entity.getErrandType()).isNull();
		assertThat(entity.getLanguage()).isEqualTo("en");
		assertThat(entity.getVersion()).isEqualTo("6.0");
		assertThat(entity.getTags()).isEmpty();

		activity.withPart(Part.create().withArende(Arende.create().withIdentifierare("A").withTyp("Diarienummer")));
		ActivityMapper.toEntity(MUNICIPALITY_ID, PRODUCER, "6.1", activity, entity);
		assertThat(entity.getCustomerIdentifier()).isNull();
		assertThat(entity.getCustomerType()).isNull();
		assertThat(entity.getCustomerSuffix()).isNull();
	}

	@Test
	void toKundhandelseAndPart() {
		final var entity = entity("TESTKOP-1").withExtendedInformation("{\"senastDatum\":\"2026-09-30\"}");

		final var kundhandelse = ActivityMapper.toKundhandelse(entity);
		final var part = ActivityMapper.toPart(entity);

		assertThat(kundhandelse.getTidpunkt()).isEqualTo("2026-09-20T10:00:00+02:00");
		assertThat(kundhandelse.getUtokadInformation().get("senastDatum").asString()).isEqualTo("2026-09-30");
		assertThat(kundhandelse.getReferenser().get("diarienummer").asString()).isEqualTo("BYGG-1");
		assertThat(kundhandelse.isProducentarendetKlart()).isTrue();
		assertThat(kundhandelse.isProducentarendetKraverKundatgard()).isFalse();
		assertThat(part.getKund().getIdentifierare()).isEqualTo("199009090000");
		assertThat(part.getArende().getIdentifierare()).isEqualTo("BYGG-1");

		final var withoutParty = ActivityMapper.toPart(entity.withCustomerIdentifier(null).withErrandIdentifier(null));
		assertThat(withoutParty.isEmpty()).isTrue();
	}

	@Test
	void toCachadKundhandelse() {
		final var cached = ActivityMapper.toCachadKundhandelse(entity("TESTKOP-1"));

		assertThat(cached.getKundhandelse().getKundhandelseId()).isEqualTo("TESTKOP-1");
		assertThat(cached.getPart().getKund().getTyp()).isEqualTo("Personnummer");
		assertThat(cached.getTaggar()).containsExactly("bygglov");
	}

	@Test
	void json() {
		assertThat(ActivityMapper.writeJson(null)).isNull();
		assertThat(ActivityMapper.writeJson(NullNode.getInstance())).isNull();
		assertThat(ActivityMapper.writeJson(MissingNode.getInstance())).isNull();
		assertThat(ActivityMapper.writeJson(JsonNodeFactory.instance.objectNode().put("a", 1))).isEqualTo("{\"a\":1}");
		assertThat(ActivityMapper.readJson(null)).isNull();
		assertThatIllegalStateException().isThrownBy(() -> ActivityMapper.readJson("{not json"));
	}
}
