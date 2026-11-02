package se.sundsvall.activityfeed.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class ProducerServiceTest {

	private static final Producer PRODUCER = new Producer("2281", "Testköpings kommun", "TESTKOP");

	private final ProducerService service = new ProducerService(new ActivityFeedProperties("6.2", false, List.of(PRODUCER)));

	@Test
	void getProducer() {
		assertThat(service.getProducer("2281")).isEqualTo(PRODUCER);
		assertThat(service.standardVersion()).isEqualTo("6.2");
	}

	@Test
	void unknownMunicipalityGives404() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.getProducer("1480"))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).isEqualTo("Kommun 1480 är inte konfigurerad som producent");
			});
	}
}
