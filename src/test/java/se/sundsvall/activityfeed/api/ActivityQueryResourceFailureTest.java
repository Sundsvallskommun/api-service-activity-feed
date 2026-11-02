package se.sundsvall.activityfeed.api;

import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.activityfeed.service.ActivityQueryService;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Errors from the query interface use Skatteverket's format {"message": "..."} and name the field.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ActivityQueryResourceFailureTest {

	private static final String VALID_QUERY = """
		{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
		""";

	@MockitoBean
	private ActivityQueryService activityQueryService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void correlationIdMissing() {
		webTestClient.post().uri(ActivityQueryResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue(VALID_QUERY)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("""
				{"message":"Bad request: headern skv_client_correlation_id saknas"}
				""", true);
		verifyNoInteractions(activityQueryService);
	}

	@Test
	void correlationIdInvalid() {
		for (final var id : new String[] {
			"x".repeat(201), "åäö"
		}) {
			postQuery(id, VALID_QUERY, "Bad request: skv_client_correlation_id: måste vara en icke-tom sträng med högst 200 skrivbara tecken");
		}
		verifyNoInteractions(activityQueryService);
	}

	@Test
	void fieldValidationNamesTheFields() {
		postQuery("abc", """
			{"fraga":{"parter":[{"kund":{"identifierare":"12345","typ":"Personnummer"}}]},"anvandare":"abc"}
			""", "Bad request: anvandare: anvandare ska vara 12 siffror; fraga.parter[0].kund.identifierare: kund.identifierare ska vara 12 siffror");
		postQuery("abc", """
			{"fraga":{"parter":[]},"anvandare":"199009090000"}
			""", "Bad request: fraga.parter: fraga.parter måste innehålla minst en part");
		verifyNoInteractions(activityQueryService);
	}

	@Test
	void queryLimits() {
		final var parties = Collections.nCopies(101, "{\"kund\":{\"identifierare\":\"199009090000\",\"typ\":\"Personnummer\"}}").stream().collect(Collectors.joining(","));
		postQuery("abc", "{\"fraga\":{\"parter\":[" + parties + "]},\"anvandare\":\"199009090000\"}",
			"Bad request: fraga.parter: fraga.parter får innehålla högst 100 parter");
		postQuery("abc", """
			{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],
			 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}],"paginering":{"offset":0,"limit":5000}}},"anvandare":"199009090000"}
			""", "Bad request: fraga.behandling.paginering.limit: paginering.limit får vara högst 1000");
		postQuery("abc", """
			{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["testkop.bygglov"]},"anvandare":"199009090000"}
			""", "Bad request: fraga.kundhandelseTyper[0]: kundhandelseTyper har ogiltigt format");
		verifyNoInteractions(activityQueryService);
	}

	@Test
	void malformedJson() {
		postQuery("abc", "{not json", "Bad request: meddelandet kan inte tolkas som JSON");
		postQuery("abc", """
			{"fraga":{"parter":"fel typ"},"anvandare":"199009090000"}
			""", "Bad request: fältet fraga.parter har fel typ eller format");
		postQuery("abc", """
			{"fraga":{"parter":[{"kund":"fel typ"}]},"anvandare":"199009090000"}
			""", "Bad request: fältet fraga.parter[0].kund har fel typ eller format");
		verifyNoInteractions(activityQueryService);
	}

	@Test
	void serviceErrorBecomesMessage() {
		when(activityQueryService.answer(anyString(), any(), any())).thenThrow(Problem.valueOf(BAD_REQUEST, "Paginering kräver att sortering anges"));
		postQuery("abc", VALID_QUERY, "Bad request: Paginering kräver att sortering anges");
	}

	@Test
	void unknownMunicipalityGives404() {
		when(activityQueryService.answer(anyString(), any(), any())).thenThrow(Problem.valueOf(NOT_FOUND, "Kommun 2262 är inte konfigurerad som producent"));

		webTestClient.post().uri("/2262/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(VALID_QUERY)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().json("""
				{"message":"Not found: Kommun 2262 är inte konfigurerad som producent"}
				""", true);
	}

	@Test
	void invalidMunicipalityId() {
		webTestClient.post().uri("/abc/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(VALID_QUERY)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().jsonPath("$.message").value(message -> assertThat((String) message).startsWith("Bad request: "));
		verifyNoInteractions(activityQueryService);
	}

	@Test
	void unexpectedErrorGives500WithoutDetails() {
		when(activityQueryService.answer(anyString(), any(), any())).thenThrow(new IllegalStateException("secret internal detail"));
		postQuery("abc", VALID_QUERY, 500, "Internal server error");
	}

	private void postQuery(final String correlationId, final String body, final String expectedMessage) {
		postQuery(correlationId, body, 400, expectedMessage);
	}

	private void postQuery(final String correlationId, final String body, final int status, final String expectedMessage) {
		webTestClient.post().uri(ActivityQueryResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.header("skv_client_correlation_id", correlationId)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(status)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("{\"message\":\"" + expectedMessage.replace("\"", "\\\"") + "\"}", true);
	}
}
