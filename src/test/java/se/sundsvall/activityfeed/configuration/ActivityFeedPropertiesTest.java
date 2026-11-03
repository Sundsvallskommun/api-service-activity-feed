package se.sundsvall.activityfeed.configuration;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class ActivityFeedPropertiesTest {

	@Autowired
	private ActivityFeedProperties properties;

	@Test
	void readFromConfiguration() {
		assertThat(properties.standardVersion()).isEqualTo("6.1");
		assertThat(properties.seed()).isFalse();
		assertThat(properties.producers()).containsExactly(
			new Producer("2281", "Testköpings kommun", "TESTKOP"),
			new Producer("2262", "Timrå kommun", "TIMRA"));
	}

	@Test
	void producerPerMunicipality() {
		assertThat(properties.producer("2262")).hasValue(new Producer("2262", "Timrå kommun", "TIMRA"));
		assertThat(properties.producer("1480")).isEmpty();
	}

	@Test
	void sameMunicipalityMustNotOccurTwice() {
		final var producers = List.of(new Producer("2281", "A", "A"), new Producer("2281", "B", "B"));

		assertThatIllegalStateException()
			.isThrownBy(() -> new ActivityFeedProperties("6.1", false, producers))
			.withMessageContaining("2281");
	}

	/**
	 * The standard version is stored with every activity that has no version of its own, in a VARCHAR(10) column.
	 */
	@Test
	void standardVersionFitsTheVersionColumn() {
		final var producers = List.of(new Producer("2281", "A", "A"));

		try (final var factory = Validation.buildDefaultValidatorFactory()) {
			assertThat(factory.getValidator().validate(new ActivityFeedProperties("6.1.0.0.0.1", false, producers)))
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.containsExactly("standardVersion");
			assertThat(factory.getValidator().validate(new ActivityFeedProperties("10.1", false, producers))).isEmpty();
		}
	}

	@Test
	void withoutProducersTheListIsEmpty() {
		assertThat(new ActivityFeedProperties("6.1", false, null).producers()).isEmpty();
	}
}
