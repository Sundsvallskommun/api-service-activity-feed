package se.sundsvall.activityfeed.api;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.service.ActivityService;
import se.sundsvall.dept44.problem.Problem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static se.sundsvall.activityfeed.service.TestdataFactory.nyKundhandelse;

/**
 * The cache endpoints answer errors with RFC 9457 (dept44 Problem).
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ActivityResourceFailureTest {

	@MockitoBean
	private ActivityService activityService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void emptyList() {
		post(List.of())
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.violations[0].message").isEqualTo("listan får inte vara tom");
		verifyNoInteractions(activityService);
	}

	@Test
	void tooMany() {
		post(Collections.nCopies(1001, nyKundhandelse("TESTKOP-1")))
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.violations[0].field").isEqualTo("sparaKundhandelser.kundhandelser")
			.jsonPath("$.violations[0].message").isEqualTo("högst 1000 kundhändelser per anrop");
		verifyNoInteractions(activityService);
	}

	@Test
	void invalidFieldsInListElement() {
		post(List.of(nyKundhandelse("TESTKOP-1").withRubrik("x".repeat(256)).withKundhandelseTyp("TESTKOP..BESLUT").withVersion("6.1.0.0.0.1")))
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.violations.length()").isEqualTo(3)
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].rubrik')].message").isEqualTo("size must be between 0 and 255")
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].kundhandelseTyp')].message")
			.isEqualTo("kundhandelseTyp ska bestå av minst tre delar åtskilda av punkt, t.ex. REFKOM.BYGGLOV.BESLUT. Prefixet får innehålla A-Z, övriga delar A-Z, 0-9 och _")
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].version')].message").isEqualTo("version får vara högst 10 tecken");
		verifyNoInteractions(activityService);
	}

	/**
	 * A long value in the right format gives 400 for its length. A repeated group in a regex recurses once per repetition,
	 * so the patterns use possessive quantifiers to keep long input from overflowing the stack.
	 */
	@ParameterizedTest(name = "{1}")
	@MethodSource("longValuesInTheRightFormat")
	void longValueGives400(final NyKundhandelse activity, final String field) {
		post(List.of(activity))
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0]." + field + "')]").exists();
		verifyNoInteractions(activityService);
	}

	static Stream<Arguments> longValuesInTheRightFormat() {
		return Stream.of(
			Arguments.of(nyKundhandelse("TESTKOP-1").withVersion("1" + ".1".repeat(100_000)), "version"),
			Arguments.of(nyKundhandelse("TESTKOP-1").withKundhandelseTyp("TESTKOP" + ".A".repeat(100_000)), "kundhandelseTyp"),
			Arguments.of(nyKundhandelse("TESTKOP-1").withSprak("sv" + "-ab".repeat(100_000)), "sprak"));
	}

	/**
	 * Local time without an offset is ambiguous when summer time ends, so neither of the forms accepted before is.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"2026-09-20T10:00:00", "2026-09-20 10:00:00"
	})
	void tidpunktWithoutOffset(final String tidpunkt) {
		post("""
			[{"kundhandelseId": "TESTKOP-1", "part": {"kund": {"identifierare": "199009090000", "typ": "Personnummer"}}, "rubrik": "r", "beskrivning": "b",
			"tidpunkt": "%s", "kundhandelseTyp": "TESTKOP.TEST.SKAPAD", "producentarendetKraverKundatgard": false, "producentarendetKlart": false}]
			""".formatted(tidpunkt))
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);
		verifyNoInteractions(activityService);
	}

	@Test
	void missingActivityGives404() {
		when(activityService.read("2281", "X")).thenThrow(Problem.valueOf(NOT_FOUND, "Kundhändelsen X finns inte"));

		webTestClient.get().uri(ActivityResourceTest.PATH + "/X")
			.exchange()
			.expectStatus().isNotFound()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(404)
			.jsonPath("$.detail").isEqualTo("Kundhändelsen X finns inte");
	}

	@Test
	void concurrentIngestGives409() {
		when(activityService.save(anyString(), any())).thenThrow(Problem.valueOf(CONFLICT, "Samtidig ändring"));

		post(List.of(nyKundhandelse("TESTKOP-1")))
			.expectStatus().isEqualTo(409)
			.expectBody().jsonPath("$.status").isEqualTo(409);
	}

	@Test
	void invalidMunicipalityId() {
		webTestClient.get().uri("/abc/kundhandelser")
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.violations[0].field").isEqualTo("antalKundhandelser.municipalityId");
		verifyNoInteractions(activityService);
	}

	private WebTestClient.ResponseSpec post(final Object body) {
		return webTestClient.post().uri(ActivityResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange();
	}
}
