package se.sundsvall.activityfeed.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.activityfeed.apptest.Constants.CORRELATION_ID;
import static se.sundsvall.activityfeed.apptest.Constants.CORRELATION_ID_HEADER;

/**
 * The query rules (the standard and Skatteverket's API definition) against MariaDB, including the order of the results.
 *
 * @see src/test/resources/db/scripts/testdata-it.sql for test data.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("it")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ActivityQueryBehaviorIT {

	private static final String CUSTOMER = """
		{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}""";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void activityTypeMatchesPrefixAndExactType() {
		query("\"kundhandelseTyper\":[\"TESTKOP.BYGGLOV\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(2);
		query("\"kundhandelseTyper\":[\"TESTKOP.BYGGLOV.ANSOKAN_MOTTAGEN\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1);
		query("\"kundhandelseTyper\":[\"TESTKOP.BYGG\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(0);
		query("\"kundhandelseTyper\":[\"TESTKOP\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3);
	}

	@Test
	void tagsAndDates() {
		query("\"taggar\":[\"kundatgard\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(2);
		query("\"startDatum\":\"2026-09-01\",\"slutDatum\":\"2026-09-02\"")
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId").isEqualTo("TESTKOP-BYGG-2026-00123-2");
		// The date bounds are inclusive and in Swedish time: 2026-09-10 07:30 Swedish time is 05:30 UTC.
		query("\"startDatum\":\"2026-09-10\",\"slutDatum\":\"2026-09-10\"").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1);
	}

	@Test
	void sortingAndPagination() {
		query("\"behandling\":{\"sortering\":[{\"attribut\":\"TIDPUNKT\",\"stigande\":true}],\"paginering\":{\"offset\":1,\"limit\":1}}")
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart.length()").isEqualTo(1)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId").isEqualTo("TESTKOP-BYGG-2026-00123-2")
			.jsonPath("$.metadata.behandling.paginering.limit").isEqualTo(1);

		// Without limit the rest of the list is returned. Titles sort case-insensitively.
		query("\"behandling\":{\"sortering\":[{\"attribut\":\"RUBRIK\",\"stigande\":true}],\"paginering\":{\"offset\":1}}")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart.length()").isEqualTo(2)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].rubrik").isEqualTo("Ansökan om bygglov mottagen")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[1].rubrik").isEqualTo("Plats i förskola erbjuden");

		query("\"behandling\":{\"sortering\":[{\"attribut\":\"TIDPUNKT\",\"stigande\":false}],\"paginering\":{\"offset\":10,\"limit\":5}}")
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart.length()").isEqualTo(0);
	}

	@Test
	void severalPartiesAreAnsweredSeparatelyAndOnlyForTheMunicipality() {
		webTestClient.post().uri("/2281/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("""
				{"fraga":{"parter":[%s,{"kund":{"identifierare":"165560001234","typ":"Organisationsnummer"}},{"arende":{"identifierare":"FINNS-INTE","typ":"Diarienummer"}}]},"anvandare":"199009090000"}
				""".formatted(CUSTOMER))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelser.length()").isEqualTo(3)
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3)
			.jsonPath("$.kundhandelser[1].totaltAntalKundhandelser").isEqualTo(2)
			.jsonPath("$.kundhandelser[2].totaltAntalKundhandelser").isEqualTo(0)
			.jsonPath("$.delfragor.length()").isEqualTo(3);

		// The same customer in municipality 2262 only gives Timrå's activity.
		webTestClient.post().uri("/2262/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("{\"fraga\":{\"parter\":[" + CUSTOMER + "]},\"anvandare\":\"199009090000\"}")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].producent").isEqualTo("Timrå kommun")
			.jsonPath("$.delfragor[0].producent").isEqualTo("Timrå kommun");
	}

	@Test
	void timestampSortsChronologicallyEvenWhenOffsetDiffers() {
		// The hour 02:00-03:00 on 25 October 2026 occurs twice in Swedish time. As text "+01:00" sorts before "+02:00",
		// but chronologically +02:00 (summer time) comes first. Sorting must use the point in time, not the text.
		webTestClient.post().uri("/2281/kundhandelser")
			.contentType(APPLICATION_JSON)
			.bodyValue("""
				[{"kundhandelseId":"DST-VINTER","part":{"kund":{"identifierare":"197001010001","typ":"Personnummer"}},
				  "rubrik":"Efter omställning","beskrivning":"b","tidpunkt":"2026-10-25T02:30:00+01:00",
				  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false},
				 {"kundhandelseId":"DST-SOMMAR","part":{"kund":{"identifierare":"197001010001","typ":"Personnummer"}},
				  "rubrik":"Före omställning","beskrivning":"b","tidpunkt":"2026-10-25T02:30:00+02:00",
				  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
				""")
			.exchange()
			.expectStatus().isCreated();

		webTestClient.post().uri("/2281/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("""
				{"fraga":{"parter":[{"kund":{"identifierare":"197001010001","typ":"Personnummer"}}],
				 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}]}},"anvandare":"197001010001"}
				""")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId").isEqualTo("DST-SOMMAR")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].tidpunkt").isEqualTo("2026-10-25T02:30:00+02:00")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[1].kundhandelseId").isEqualTo("DST-VINTER")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[1].tidpunkt").isEqualTo("2026-10-25T02:30:00+01:00");
	}

	@Test
	void sameActivityIdReplacesInsteadOfDuplicating() {
		final var activity = """
			[{"kundhandelseId":"TEST-1","part":{"kund":{"identifierare":"198001010101","typ":"Personnummer"}},
			  "rubrik":"%s","beskrivning":"Beskrivning","tidpunkt":"2026-09-15 12:00:00",
			  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
			""";
		for (final var title : new String[] {
			"Testhändelse", "Uppdaterad"
		}) {
			webTestClient.post().uri("/2281/kundhandelser")
				.contentType(APPLICATION_JSON)
				.bodyValue(activity.formatted(title))
				.exchange()
				.expectStatus().isCreated();
		}

		webTestClient.get().uri("/2281/kundhandelser/TEST-1")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelse.rubrik").isEqualTo("Uppdaterad")
			.jsonPath("$.kundhandelse.tidpunkt").isEqualTo("2026-09-15T12:00:00+02:00");

		webTestClient.get().uri("/2281/kundhandelser")
			.exchange()
			.expectBody().jsonPath("$.antal").isEqualTo(6);
	}

	private WebTestClient.BodyContentSpec query(final String filter) {
		return webTestClient.post().uri("/2281/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("{\"fraga\":{\"parter\":[" + CUSTOMER + "]," + filter + "},\"anvandare\":\"199009090000\"}")
			.exchange()
			.expectStatus().isOk()
			.expectBody();
	}
}
