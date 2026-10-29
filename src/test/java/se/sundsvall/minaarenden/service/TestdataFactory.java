package se.sundsvall.minaarenden.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import se.sundsvall.minaarenden.api.model.Arende;
import se.sundsvall.minaarenden.api.model.Fraga;
import se.sundsvall.minaarenden.api.model.Kund;
import se.sundsvall.minaarenden.api.model.KundhandelseFragaRequest;
import se.sundsvall.minaarenden.api.model.NyKundhandelse;
import se.sundsvall.minaarenden.api.model.Part;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;
import tools.jackson.databind.node.JsonNodeFactory;

public final class TestdataFactory {

	public static final String MUNICIPALITY_ID = "2281";
	public static final Producent PRODUCENT = new Producent(MUNICIPALITY_ID, "Testköpings kommun", "TESTKOP");

	private TestdataFactory() {}

	public static Kund kund() {
		return Kund.create().withIdentifierare("199009090000").withTyp("Personnummer");
	}

	public static Part part() {
		return Part.create().withKund(kund()).withArende(Arende.create().withIdentifierare("BYGG-1").withTyp("Diarienummer"));
	}

	public static KundhandelseFragaRequest fraga() {
		return KundhandelseFragaRequest.create()
			.withAnvandare("199009090000")
			.withFraga(Fraga.create().withParter(List.of(part())));
	}

	public static NyKundhandelse nyKundhandelse(final String kundhandelseId) {
		return NyKundhandelse.create()
			.withKundhandelseId(kundhandelseId)
			.withPart(part())
			.withRubrik("Beslut om bygglov")
			.withBeskrivning("Bygglov har beviljats.")
			.withTidpunkt("2026-09-20T10:00:00+02:00")
			.withKundhandelseTyp("TESTKOP.BYGGLOV.BESLUT")
			.withProducentarendetKraverKundatgard(false)
			.withProducentarendetKlart(true)
			.withReferenser(JsonNodeFactory.instance.objectNode().put("diarienummer", "BYGG-1"))
			.withTaggar(List.of("bygglov"));
	}

	public static KundhandelseEntity entity(final String kundhandelseId) {
		return KundhandelseEntity.create()
			.withId("5d8c3e0a-1b7f-4c2e-9a6d-3f1e2b4c5a6d")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withKundhandelseId(kundhandelseId)
			.withProducent(PRODUCENT.namn())
			.withKundIdentifierare("199009090000")
			.withKundTyp("Personnummer")
			.withArendeIdentifierare("BYGG-1")
			.withArendeTyp("Diarienummer")
			.withRubrik("Beslut om bygglov")
			.withBeskrivning("Bygglov har beviljats.")
			.withSprak("sv")
			.withTidpunkt(Instant.parse("2026-09-20T08:00:00Z"))
			.withKundhandelseTyp("TESTKOP.BYGGLOV.BESLUT")
			.withProducentarendetKraverKundatgard(false)
			.withProducentarendetKlart(true)
			.withVersion("6.1")
			.withReferenser("{\"diarienummer\":\"BYGG-1\"}")
			.withTaggar(Set.of("bygglov"));
	}
}
