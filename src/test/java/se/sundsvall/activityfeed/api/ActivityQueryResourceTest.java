package se.sundsvall.activityfeed.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.activityfeed.api.model.Delfraga;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaRequest;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaResponse;
import se.sundsvall.activityfeed.service.ActivityQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.activityfeed.service.TestdataFactory.fraga;

@AutoConfigureWebTestClient
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ActivityQueryResourceTest {

	static final String PATH = "/2281/kundhandelseFragaSynkron";
	static final String API_KEY = "junit-query-key-0123456789abcdef0123456789";

	@MockitoBean
	private ActivityQueryService activityQueryService;

	@Autowired
	private WebTestClient webTestClient;

	@Captor
	private ArgumentCaptor<KundhandelseFragaRequest> requestCaptor;

	@Test
	void kundhandelseFragaSynkron() {
		final var request = fraga();
		final var expected = KundhandelseFragaResponse.create()
			.withKundhandelser(List.of())
			.withDelfragor(List.of(Delfraga.create().withProducent("Testköpings kommun").withStatus("OK").withHttpCode(200)));
		when(activityQueryService.answer(eq("2281"), requestCaptor.capture(), eq("sv"))).thenReturn(expected);

		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", API_KEY)
			.header("skv_client_correlation_id", "0002aa29-49f2-4baf-be51-c7c39c9824b4")
			.header("Accept-Language", "sv")
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(KundhandelseFragaResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(expected);
		assertThat(requestCaptor.getValue()).isEqualTo(request);
		verify(activityQueryService).answer(eq("2281"), eq(request), eq("sv"));
	}

	@Test
	void correlationIdDoesNotHaveToBeUuid() {
		when(activityQueryService.answer(eq("2281"), requestCaptor.capture(), eq(null))).thenReturn(KundhandelseFragaResponse.create());

		webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", API_KEY)
			.header("skv_client_correlation_id", "anrop-4711")
			.bodyValue(fraga())
			.exchange()
			.expectStatus().isOk();
	}
}
