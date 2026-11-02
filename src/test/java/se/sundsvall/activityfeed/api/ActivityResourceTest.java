package se.sundsvall.activityfeed.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.activityfeed.api.model.AntalKundhandelser;
import se.sundsvall.activityfeed.api.model.CachadKundhandelse;
import se.sundsvall.activityfeed.api.model.Kundhandelse;
import se.sundsvall.activityfeed.service.ActivityService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.activityfeed.service.TestdataFactory.nyKundhandelse;
import static se.sundsvall.activityfeed.service.TestdataFactory.part;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ActivityResourceTest {

	static final String PATH = "/2281/kundhandelser";
	static final String API_KEY = "junit-admin-key-0123456789abcdef0123456789";

	@MockitoBean
	private ActivityService activityService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sparaKundhandelser() {
		final var activities = List.of(nyKundhandelse("TESTKOP-1"), nyKundhandelse("TESTKOP-2"));
		when(activityService.save("2281", activities)).thenReturn(2);

		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", API_KEY)
			.bodyValue(activities)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(AntalKundhandelser.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(new AntalKundhandelser(2));
		verify(activityService).save("2281", activities);
	}

	@Test
	void hamtaKundhandelse() {
		final var cached = CachadKundhandelse.create().withPart(part()).withKundhandelse(Kundhandelse.create().withKundhandelseId("TESTKOP-1")).withTaggar(List.of("bygglov"));
		when(activityService.read("2281", "TESTKOP-1")).thenReturn(cached);

		final var response = webTestClient.get().uri(PATH + "/TESTKOP-1")
			.header("X-API-Key", API_KEY)
			.exchange()
			.expectStatus().isOk()
			.expectBody(CachadKundhandelse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(cached);
	}

	@Test
	void taBortKundhandelse() {
		webTestClient.delete().uri(PATH + "/TESTKOP-1")
			.header("X-API-Key", API_KEY)
			.exchange()
			.expectStatus().isNoContent();

		verify(activityService).delete("2281", "TESTKOP-1");
	}

	@Test
	void antalKundhandelser() {
		when(activityService.count("2281")).thenReturn(5L);

		webTestClient.get().uri(PATH)
			.header("X-API-Key", API_KEY)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json("""
				{"antal":5}
				""", true);
	}
}
