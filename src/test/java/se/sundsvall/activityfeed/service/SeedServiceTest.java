package se.sundsvall.activityfeed.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.activityfeed.integration.db.ActivityRepository;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedServiceTest {

	private static final Producer TESTKOP = new Producer("2281", "Testköpings kommun", "TESTKOP");
	private static final Producer TIMRA = new Producer("2262", "Timrå kommun", "TIMRA");

	@Mock
	private ActivityService service;

	@Mock
	private ActivityRepository repository;

	@Captor
	private ArgumentCaptor<List<NyKundhandelse>> listCaptor;

	@Test
	void samplesAreLoadedOnlyForMunicipalitiesWithoutData() throws Exception {
		when(repository.countByMunicipalityId("2281")).thenReturn(0L);
		when(repository.countByMunicipalityId("2262")).thenReturn(3L);
		when(service.save(eq("2281"), any())).thenReturn(5);

		seedService(true).run(null);

		verify(service).save(eq("2281"), listCaptor.capture());
		verify(service, never()).save(eq("2262"), any());
		assertThat(listCaptor.getValue()).hasSize(5).allSatisfy(sample -> {
			assertThat(sample.getKundhandelseId()).startsWith("TESTKOP-");
			assertThat(sample.getKundhandelseTyp()).startsWith("TESTKOP.");
		});
	}

	@Test
	void withoutSeedNothingIsLoaded() throws Exception {
		when(repository.countByMunicipalityId(anyString())).thenReturn(0L);

		seedService(false).run(null);

		verify(repository).countByMunicipalityId("2281");
		verify(repository).countByMunicipalityId("2262");
		verify(service, never()).save(anyString(), any());
	}

	@Test
	void prefixIsReplacedOnlyInIdAndType() {
		final var sample = NyKundhandelse.create()
			.withKundhandelseId("REFKOM-BYGG-1")
			.withKundhandelseTyp("REFKOM.BYGGLOV.BESLUT")
			.withRubrik("REFKOM nämns i rubriken");

		SeedService.replacePrefix(sample, "TESTKOP");

		assertThat(sample.getKundhandelseId()).isEqualTo("TESTKOP-BYGG-1");
		assertThat(sample.getKundhandelseTyp()).isEqualTo("TESTKOP.BYGGLOV.BESLUT");
		assertThat(sample.getRubrik()).isEqualTo("REFKOM nämns i rubriken");
		assertThat(SeedService.replacePrefix(NyKundhandelse.create(), "X").getKundhandelseId()).isNull();
	}

	private SeedService seedService(final boolean seed) {
		return new SeedService(new ActivityFeedProperties("6.1", seed, List.of(TESTKOP, TIMRA)), service, repository, JsonMapper.shared());
	}
}
