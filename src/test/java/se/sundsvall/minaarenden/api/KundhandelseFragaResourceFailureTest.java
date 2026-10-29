package se.sundsvall.minaarenden.api;

import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.minaarenden.Application;
import se.sundsvall.minaarenden.service.FragaService;

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
 * Felsvar från frågegränssnittet har Skatteverkets format {"message": "..."} och pekar ut fältet.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class KundhandelseFragaResourceFailureTest {

	private static final String GILTIG_FRAGA = """
		{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
		""";

	@MockitoBean
	private FragaService fragaService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void correlationIdSaknas() {
		webTestClient.post().uri(KundhandelseFragaResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", KundhandelseFragaResourceTest.API_KEY)
			.bodyValue(GILTIG_FRAGA)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("""
				{"message":"Bad request: headern skv_client_correlation_id saknas"}
				""", true);
		verifyNoInteractions(fragaService);
	}

	@Test
	void correlationIdOgiltigt() {
		for (final var id : new String[] {
			"x".repeat(201), "åäö"
		}) {
			fraga(id, GILTIG_FRAGA, "Bad request: skv_client_correlation_id: måste vara en icke-tom sträng med högst 200 skrivbara tecken");
		}
		verifyNoInteractions(fragaService);
	}

	@Test
	void faltValideringPekarUtFalten() {
		fraga("abc", """
			{"fraga":{"parter":[{"kund":{"identifierare":"12345","typ":"Personnummer"}}]},"anvandare":"abc"}
			""", "Bad request: anvandare: anvandare ska vara 12 siffror; fraga.parter[0].kund.identifierare: kund.identifierare ska vara 12 siffror");
		fraga("abc", """
			{"fraga":{"parter":[]},"anvandare":"199009090000"}
			""", "Bad request: fraga.parter: fraga.parter måste innehålla minst en part");
		verifyNoInteractions(fragaService);
	}

	@Test
	void granserForFragan() {
		final var parter = Collections.nCopies(101, "{\"kund\":{\"identifierare\":\"199009090000\",\"typ\":\"Personnummer\"}}").stream().collect(Collectors.joining(","));
		fraga("abc", "{\"fraga\":{\"parter\":[" + parter + "]},\"anvandare\":\"199009090000\"}",
			"Bad request: fraga.parter: fraga.parter får innehålla högst 100 parter");
		fraga("abc", """
			{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],
			 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}],"paginering":{"offset":0,"limit":5000}}},"anvandare":"199009090000"}
			""", "Bad request: fraga.behandling.paginering.limit: paginering.limit får vara högst 1000");
		fraga("abc", """
			{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["testkop.bygglov"]},"anvandare":"199009090000"}
			""", "Bad request: fraga.kundhandelseTyper[0]: kundhandelseTyper har ogiltigt format");
		verifyNoInteractions(fragaService);
	}

	@Test
	void felaktigJson() {
		fraga("abc", "{inte json", "Bad request: meddelandet kan inte tolkas som JSON");
		fraga("abc", """
			{"fraga":{"parter":"fel typ"},"anvandare":"199009090000"}
			""", "Bad request: fältet fraga.parter har fel typ eller format");
		fraga("abc", """
			{"fraga":{"parter":[{"kund":"fel typ"}]},"anvandare":"199009090000"}
			""", "Bad request: fältet fraga.parter[0].kund har fel typ eller format");
		verifyNoInteractions(fragaService);
	}

	@Test
	void felFranTjanstenBlirMessage() {
		when(fragaService.besvara(anyString(), any(), any())).thenThrow(Problem.valueOf(BAD_REQUEST, "Paginering kräver att sortering anges"));
		fraga("abc", GILTIG_FRAGA, "Bad request: Paginering kräver att sortering anges");
	}

	@Test
	void okandKommunGer404() {
		when(fragaService.besvara(anyString(), any(), any())).thenThrow(Problem.valueOf(NOT_FOUND, "Kommun 2262 är inte konfigurerad som producent"));

		webTestClient.post().uri("/2262/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", KundhandelseFragaResourceTest.API_KEY)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(GILTIG_FRAGA)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().json("""
				{"message":"Not found: Kommun 2262 är inte konfigurerad som producent"}
				""", true);
	}

	@Test
	void ogiltigtKommunId() {
		webTestClient.post().uri("/abc/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", KundhandelseFragaResourceTest.API_KEY)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(GILTIG_FRAGA)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().jsonPath("$.message").value(message -> assertThat((String) message).startsWith("Bad request: "));
		verifyNoInteractions(fragaService);
	}

	@Test
	void ovantatFelGer500UtanDetaljer() {
		when(fragaService.besvara(anyString(), any(), any())).thenThrow(new IllegalStateException("hemlig intern detalj"));
		fraga("abc", GILTIG_FRAGA, 500, "Internal server error");
	}

	private void fraga(final String correlationId, final String body, final String forvantatMeddelande) {
		fraga(correlationId, body, 400, forvantatMeddelande);
	}

	private void fraga(final String correlationId, final String body, final int status, final String forvantatMeddelande) {
		webTestClient.post().uri(KundhandelseFragaResourceTest.PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", KundhandelseFragaResourceTest.API_KEY)
			.header("skv_client_correlation_id", correlationId)
			.bodyValue(body)
			.exchange()
			.expectStatus().isEqualTo(status)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("{\"message\":\"" + forvantatMeddelande.replace("\"", "\\\"") + "\"}", true);
	}
}
