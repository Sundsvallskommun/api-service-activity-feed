package se.sundsvall.minaarenden.api;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.minaarenden.Application;
import se.sundsvall.minaarenden.service.KundhandelseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static se.sundsvall.minaarenden.service.TestdataFactory.nyKundhandelse;

/**
 * Ärendecachen svarar med RFC 9457 (dept44 Problem) vid fel.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class KundhandelseResourceFailureTest {

	@MockitoBean
	private KundhandelseService kundhandelseService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void tomLista() {
		post(List.of())
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.violations[0].message").isEqualTo("listan får inte vara tom");
		verifyNoInteractions(kundhandelseService);
	}

	@Test
	void forManga() {
		post(Collections.nCopies(1001, nyKundhandelse("TESTKOP-1")))
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Constraint Violation")
			.jsonPath("$.violations[0].field").isEqualTo("sparaKundhandelser.kundhandelser")
			.jsonPath("$.violations[0].message").isEqualTo("högst 1000 kundhändelser per anrop");
		verifyNoInteractions(kundhandelseService);
	}

	@Test
	void ogiltigaFaltIListelement() {
		post(List.of(nyKundhandelse("TESTKOP-1").withRubrik("x".repeat(256)).withKundhandelseTyp("TESTKOP..BESLUT")))
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.violations.length()").isEqualTo(2)
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].rubrik')].message").isEqualTo("size must be between 0 and 255")
			.jsonPath("$.violations[?(@.field == 'sparaKundhandelser.kundhandelser[0].kundhandelseTyp')]").exists();
		verifyNoInteractions(kundhandelseService);
	}

	@Test
	void saknadKundhandelseGer404() {
		when(kundhandelseService.hamta("2281", "X")).thenThrow(Problem.valueOf(NOT_FOUND, "Kundhändelsen X finns inte"));

		webTestClient.get().uri(KundhandelseResourceTest.PATH + "/X")
			.header("X-API-Key", KundhandelseResourceTest.API_KEY)
			.exchange()
			.expectStatus().isNotFound()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(404)
			.jsonPath("$.detail").isEqualTo("Kundhändelsen X finns inte");
	}

	@Test
	void samtidigInlasningGer409() {
		when(kundhandelseService.spara(anyString(), any())).thenThrow(Problem.valueOf(CONFLICT, "Samtidig ändring"));

		post(List.of(nyKundhandelse("TESTKOP-1")))
			.expectStatus().isEqualTo(409)
			.expectBody().jsonPath("$.status").isEqualTo(409);
	}

	@Test
	void ogiltigtKommunId() {
		webTestClient.get().uri("/abc/kundhandelser")
			.header("X-API-Key", KundhandelseResourceTest.API_KEY)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.violations[0].field").isEqualTo("antalKundhandelser.municipalityId");
		verifyNoInteractions(kundhandelseService);
	}

	private WebTestClient.ResponseSpec post(final Object body) {
		return webTestClient.post().uri(KundhandelseResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", KundhandelseResourceTest.API_KEY)
			.bodyValue(body)
			.exchange();
	}
}
