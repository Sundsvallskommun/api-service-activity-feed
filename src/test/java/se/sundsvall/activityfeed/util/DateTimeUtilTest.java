package se.sundsvall.activityfeed.util;

import java.time.DateTimeException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DateTimeUtilTest {

	@ParameterizedTest
	@CsvSource({
		"2026-09-15T12:00:00+02:00, 2026-09-15T10:00:00Z",
		"2026-09-15T10:00:00Z, 2026-09-15T10:00:00Z",
		"2026-09-15T12:00:00, 2026-09-15T10:00:00Z",
		"2026-09-15 12:00:00, 2026-09-15T10:00:00Z",
		"2026-12-15 12:00:00, 2026-12-15T11:00:00Z"
	})
	void parse(final String value, final String expected) {
		assertThat(DateTimeUtil.parse(value)).isEqualTo(Instant.parse(expected));
	}

	@Test
	void parseInvalid() {
		assertThatExceptionOfType(DateTimeException.class).isThrownBy(() -> DateTimeUtil.parse("yesterday"));
	}

	@Test
	void formatWithSwedishOffset() {
		assertThat(DateTimeUtil.format(Instant.parse("2026-09-15T10:00:00Z"))).isEqualTo("2026-09-15T12:00:00+02:00");
		assertThat(DateTimeUtil.format(Instant.parse("2026-12-15T11:00:00Z"))).isEqualTo("2026-12-15T12:00:00+01:00");
	}

	@Test
	void startAndEndOfDayInSwedishTime() {
		assertThat(DateTimeUtil.startOfDay("2026-09-01")).isEqualTo(Instant.parse("2026-08-31T22:00:00Z"));
		assertThat(DateTimeUtil.endOfDay("2026-09-01")).isEqualTo(Instant.parse("2026-09-01T21:59:59.999Z"));
		assertThat(DateTimeUtil.startOfDay(null)).isNull();
		assertThat(DateTimeUtil.endOfDay(null)).isNull();
	}
}
