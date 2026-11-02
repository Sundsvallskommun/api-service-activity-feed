package se.sundsvall.activityfeed.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ApiKeyPropertiesTest {

	private static final String KEY_1 = "a".repeat(ApiKeyProperties.MIN_LENGTH);
	private static final String KEY_2 = "b".repeat(64);

	@Test
	void keysAreTrimmedAndDuplicatesRemoved() {
		final var properties = new ApiKeyProperties("X-API-Key", List.of(" " + KEY_1 + " ", "", KEY_1), List.of(KEY_2));

		assertThat(properties.queryKeys()).containsExactly(KEY_1);
		assertThat(properties.adminKeys()).containsExactly(KEY_2);
		assertThat(properties.headerName()).isEqualTo("X-API-Key");
	}

	@Test
	void oneKindOfKeyIsEnough() {
		assertThat(new ApiKeyProperties("X-API-Key", null, List.of(KEY_2)).queryKeys()).isEmpty();
		assertThat(new ApiKeyProperties("X-API-Key", List.of(KEY_1), null).adminKeys()).isEmpty();
	}

	@Test
	void serviceDoesNotStartWithoutKeys() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ApiKeyProperties("X-API-Key", List.of(), List.of(" ")))
			.withMessageContaining("No API key is configured");
	}

	@Test
	void tooShortKeyIsRejectedWithoutRevealingIt() {
		final var tooShort = "secret-but-too-short";

		assertThatIllegalStateException()
			.isThrownBy(() -> new ApiKeyProperties("X-API-Key", List.of(KEY_1), List.of(KEY_2, tooShort)))
			.withMessageContaining("Key number 2 in API_KEYS_ADMIN")
			.withMessageNotContaining(tooShort);
	}

	@Test
	void keyWithSpaceIsRejected() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ApiKeyProperties("X-API-Key", List.of("a".repeat(20) + " " + "b".repeat(20)), List.of()))
			.withMessageContaining("API_KEYS_QUERY");
	}

	@Test
	void headerIsRequired() {
		assertThatIllegalStateException().isThrownBy(() -> new ApiKeyProperties(" ", List.of(KEY_1), List.of()));
		assertThatIllegalStateException().isThrownBy(() -> new ApiKeyProperties(null, List.of(KEY_1), List.of()));
	}
}
