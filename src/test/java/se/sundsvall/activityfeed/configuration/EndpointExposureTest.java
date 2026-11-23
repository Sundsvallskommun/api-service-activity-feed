package se.sundsvall.activityfeed.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaResponse;
import se.sundsvall.activityfeed.service.ActivityQueryService;
import se.sundsvall.activityfeed.service.ActivityService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * What the service exposes through the whole filter chain: both interfaces, actuator, documentation, and the error
 * format for a wrong method or media type.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class EndpointExposureTest {

	private static final String QUERY_PATH = "/2281/kundhandelseFragaSynkron";
	private static final String ACTIVITIES_PATH = "/2281/kundhandelser";
	private static final String QUERY = """
		{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
		""";

	@MockitoBean
	private ActivityQueryService activityQueryService;

	@MockitoBean
	private ActivityService activityService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void bothInterfacesAnswerWithoutCredentials() {
		when(activityQueryService.answer(eq("2281"), any(), any())).thenReturn(KundhandelseFragaResponse.create());
		when(activityService.count("2281")).thenReturn(3L);

		webTestClient.post().uri(QUERY_PATH)
			.contentType(APPLICATION_JSON)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(QUERY)
			.exchange()
			.expectStatus().isOk();

		webTestClient.get().uri(ACTIVITIES_PATH)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.antal").isEqualTo(3);
	}

	@Test
	void healthShowsDetails() {
		webTestClient.get().uri("/actuator/health")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.status").isEqualTo("UP")
			.jsonPath("$.components.db.status").isEqualTo("UP");

		webTestClient.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
		webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
	}

	/**
	 * dept44's defaults: every endpoint except heapdump, threaddump and shutdown.
	 */
	@Test
	void actuatorFollowsDept44Defaults() {
		for (final var endpoint : new String[] {
			"flyway", "env", "loggers", "mappings"
		}) {
			webTestClient.get().uri("/actuator/" + endpoint).exchange().expectStatus().isOk();
		}
		for (final var endpoint : new String[] {
			"heapdump", "threaddump", "shutdown"
		}) {
			webTestClient.get().uri("/actuator/" + endpoint).exchange().expectStatus().isNotFound();
		}
	}

	@Test
	void documentationIsOpen() {
		webTestClient.get().uri("/api-docs").exchange().expectStatus().isOk();
		webTestClient.get().uri("/").exchange().expectStatus().isOk();
	}

	@Test
	void unknownPathGives404() {
		webTestClient.get().uri("/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@Test
	void wrongMethodAndContentTypeOnQuery() {
		webTestClient.get().uri(QUERY_PATH)
			.exchange()
			.expectStatus().isEqualTo(405)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("""
				{"message":"Method not allowed"}
				""", true);

		webTestClient.post().uri(QUERY_PATH)
			.header("skv_client_correlation_id", "abc")
			.contentType(TEXT_PLAIN)
			.bodyValue("text")
			.exchange()
			.expectStatus().isEqualTo(415)
			.expectBody().json("""
				{"message":"Unsupported media type"}
				""", true);
	}

	@Test
	void wrongMethodOnCacheGivesProblem() {
		webTestClient.put().uri(ACTIVITIES_PATH)
			.contentType(APPLICATION_JSON)
			.bodyValue("[]")
			.exchange()
			.expectStatus().isEqualTo(405)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.status").isEqualTo(405);
	}
}
