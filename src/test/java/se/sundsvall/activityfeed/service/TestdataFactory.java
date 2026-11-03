package se.sundsvall.activityfeed.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import se.sundsvall.activityfeed.api.model.Arende;
import se.sundsvall.activityfeed.api.model.Fraga;
import se.sundsvall.activityfeed.api.model.Kund;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaRequest;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.api.model.Part;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Test data. Methods that build API model objects are named after the API types.
 */
public final class TestdataFactory {

	public static final String MUNICIPALITY_ID = "2281";
	public static final Producer PRODUCER = new Producer(MUNICIPALITY_ID, "Testköpings kommun", "TESTKOP");

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
			.withTidpunkt(OffsetDateTime.parse("2026-09-20T10:00:00+02:00"))
			.withKundhandelseTyp("TESTKOP.BYGGLOV.BESLUT")
			.withProducentarendetKraverKundatgard(false)
			.withProducentarendetKlart(true)
			.withReferenser(JsonNodeFactory.instance.objectNode().put("diarienummer", "BYGG-1"))
			.withTaggar(List.of("bygglov"));
	}

	public static ActivityEntity entity(final String activityId) {
		return ActivityEntity.create()
			.withId("5d8c3e0a-1b7f-4c2e-9a6d-3f1e2b4c5a6d")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withActivityId(activityId)
			.withProducer(PRODUCER.name())
			.withCustomerIdentifier("199009090000")
			.withCustomerType("Personnummer")
			.withErrandIdentifier("BYGG-1")
			.withErrandType("Diarienummer")
			.withTitle("Beslut om bygglov")
			.withDescription("Bygglov har beviljats.")
			.withLanguage("sv")
			.withOccurredAt(OffsetDateTime.parse("2026-09-20T08:00:00Z"))
			.withActivityType("TESTKOP.BYGGLOV.BESLUT")
			.withCustomerActionRequired(false)
			.withErrandCompleted(true)
			.withVersion("6.1")
			.withReferenceData("{\"diarienummer\":\"BYGG-1\"}")
			.withTags(Set.of("bygglov"));
	}
}
