package se.sundsvall.activityfeed.api;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;
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
		post(List.of(nyKundhandelse("TESTKOP-1").withRubrik("x".repeat(256)).withKundhandelseTyp("TESTKOP..BESLUT")))
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.violations.length()").isEqualTo(2)
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].rubrik')].message").isEqualTo("size must be between 0 and 255")
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].kundhandelseTyp')]").exists();
		verifyNoInteractions(activityService);
	}

	@Test
	void missingActivityGives404() {
		when(activityService.read("2281", "X")).thenThrow(Problem.valueOf(NOT_FOUND, "Kundhändelsen X finns inte"));

		webTestClient.get().uri(ActivityResourceTest.PATH + "/X")
			.header("X-API-Key", ActivityResourceTest.API_KEY)
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
			.header("X-API-Key", ActivityResourceTest.API_KEY)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.violations[0].field").isEqualTo("antalKundhandelser.municipalityId");
		verifyNoInteractions(activityService);
	}

	private WebTestClient.ResponseSpec post(final Object body) {
		return webTestClient.post().uri(ActivityResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", ActivityResourceTest.API_KEY)
			.bodyValue(body)
			.exchange();
	}
}
