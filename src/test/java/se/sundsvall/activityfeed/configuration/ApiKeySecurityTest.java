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
 * API key protection through the whole filter chain. The keys come from the junit profile.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ApiKeySecurityTest {

	static final String QUERY_KEY = "junit-query-key-0123456789abcdef0123456789";
	static final String ADMIN_KEY = "junit-admin-key-0123456789abcdef0123456789";

	private static final String HEADER = "X-API-Key";
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
	void queryWithoutKeyGives401InSkatteverketFormat() {
		webTestClient.post().uri(QUERY_PATH)
			.contentType(APPLICATION_JSON)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(QUERY)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().valueEquals("WWW-Authenticate", "ApiKey header=\"X-API-Key\"")
			.expectBody().json("""
				{"message":"Unauthorized"}
				""", true);
	}

	@Test
	void queryWithWrongKeyGives401() {
		webTestClient.post().uri(QUERY_PATH)
			.contentType(APPLICATION_JSON)
			.header(HEADER, "wrong-key-0123456789abcdef0123456789abcdef")
			.header("skv_client_correlation_id", "abc")
			.bodyValue(QUERY)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectBody().jsonPath("$.message").isEqualTo("Unauthorized");
	}

	@Test
	void queryWithQueryAndAdminKey() {
		when(activityQueryService.answer(eq("2281"), any(), any())).thenReturn(KundhandelseFragaResponse.create());

		for (final var key : new String[] {
			QUERY_KEY, ADMIN_KEY
		}) {
			webTestClient.post().uri(QUERY_PATH)
				.contentType(APPLICATION_JSON)
				.header(HEADER, key)
				.header("skv_client_correlation_id", "abc")
				.bodyValue(QUERY)
				.exchange()
				.expectStatus().isOk();
		}
	}

	@Test
	void cacheEndpointsRequireAdminKey() {
		when(activityService.count("2281")).thenReturn(3L);

		webTestClient.get().uri(ACTIVITIES_PATH)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(401)
			.jsonPath("$.title").isEqualTo("Unauthorized");

		webTestClient.get().uri(ACTIVITIES_PATH + "/ID-1")
			.header(HEADER, QUERY_KEY)
			.exchange()
			.expectStatus().isForbidden()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.status").isEqualTo(403);

		webTestClient.get().uri(ACTIVITIES_PATH)
			.header(HEADER, ADMIN_KEY)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.antal").isEqualTo(3);
	}

	@Test
	void healthIsOpenButDetailsRequireAdminKey() {
		webTestClient.get().uri("/actuator/health")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.status").isEqualTo("UP")
			.jsonPath("$.components").doesNotExist();

		webTestClient.get().uri("/actuator/health")
			.header(HEADER, ADMIN_KEY)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.components.db.status").isEqualTo("UP");

		webTestClient.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
		webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
	}

	@Test
	void otherActuatorEndpointsAreClosedOrRequireAdminKey() {
		webTestClient.get().uri("/actuator/flyway").exchange().expectStatus().isUnauthorized();
		webTestClient.get().uri("/actuator/flyway").header(HEADER, QUERY_KEY).exchange().expectStatus().isForbidden();
		webTestClient.get().uri("/actuator/flyway").header(HEADER, ADMIN_KEY).exchange().expectStatus().isOk();

		for (final var endpoint : new String[] {
			"env", "beans", "configprops", "loggers", "heapdump", "threaddump", "mappings"
		}) {
			webTestClient.get().uri("/actuator/" + endpoint).header(HEADER, ADMIN_KEY).exchange().expectStatus().isNotFound();
		}
	}

	@Test
	void documentationIsOpen() {
		webTestClient.get().uri("/api-docs").exchange().expectStatus().isOk();
		webTestClient.get().uri("/").exchange().expectStatus().isOk();
	}

	@Test
	void unknownPathRequiresKey() {
		webTestClient.get().uri("/does-not-exist").exchange().expectStatus().isUnauthorized();
		webTestClient.get().uri("/does-not-exist").header(HEADER, ADMIN_KEY).exchange().expectStatus().isNotFound();
	}

	@Test
	void wrongMethodAndContentTypeOnQuery() {
		webTestClient.get().uri(QUERY_PATH)
			.header(HEADER, QUERY_KEY)
			.exchange()
			.expectStatus().isEqualTo(405)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("""
				{"message":"Method not allowed"}
				""", true);

		webTestClient.post().uri(QUERY_PATH)
			.header(HEADER, QUERY_KEY)
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
			.header(HEADER, ADMIN_KEY)
			.contentType(APPLICATION_JSON)
			.bodyValue("[]")
			.exchange()
			.expectStatus().isEqualTo(405)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.status").isEqualTo(405);
	}
}
