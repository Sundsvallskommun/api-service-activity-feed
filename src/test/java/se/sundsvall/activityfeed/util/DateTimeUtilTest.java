package se.sundsvall.activityfeed.util;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
		assertThat(DateTimeUtil.parse(value)).isAtSameInstantAs(OffsetDateTime.parse(expected));
	}

	@Test
	void parseKeepsTheGivenOffsetAndGivesLocalTimeTheSwedishOne() {
		assertThat(DateTimeUtil.parse("2026-09-15T10:00:00Z").getOffset()).isEqualTo(ZoneOffset.UTC);
		assertThat(DateTimeUtil.parse("2026-09-15 12:00:00").getOffset()).isEqualTo(ZoneOffset.ofHours(2));
		assertThat(DateTimeUtil.parse("2026-12-15T12:00:00").getOffset()).isEqualTo(ZoneOffset.ofHours(1));
	}

	@Test
	void parseInvalid() {
		assertThatExceptionOfType(DateTimeException.class).isThrownBy(() -> DateTimeUtil.parse("yesterday"));
	}

	@Test
	void formatWithSwedishOffset() {
		assertThat(DateTimeUtil.format(OffsetDateTime.parse("2026-09-15T10:00:00Z"))).isEqualTo("2026-09-15T12:00:00+02:00");
		assertThat(DateTimeUtil.format(OffsetDateTime.parse("2026-12-15T11:00:00Z"))).isEqualTo("2026-12-15T12:00:00+01:00");
	}

	@Test
	void sameLocalTimeAtSwitchToWinterTimeKeepsTheOffset() {
		// 02:00-03:00 on 25 October 2026 occurs twice in Swedish time. The offset tells the two apart.
		final var summerTime = DateTimeUtil.parse("2026-10-25T02:30:00+02:00");
		final var winterTime = DateTimeUtil.parse("2026-10-25T02:30:00+01:00");

		assertThat(summerTime).isBefore(winterTime);
		assertThat(DateTimeUtil.format(summerTime)).isEqualTo("2026-10-25T02:30:00+02:00");
		assertThat(DateTimeUtil.format(winterTime)).isEqualTo("2026-10-25T02:30:00+01:00");
	}

	@Test
	void startAndEndOfDayInSwedishTime() {
		assertThat(DateTimeUtil.startOfDay("2026-09-01")).isAtSameInstantAs(OffsetDateTime.parse("2026-08-31T22:00:00Z"));
		assertThat(DateTimeUtil.endOfDay("2026-09-01")).isAtSameInstantAs(OffsetDateTime.parse("2026-09-01T21:59:59.999Z"));
		assertThat(DateTimeUtil.startOfDay(null)).isNull();
		assertThat(DateTimeUtil.endOfDay(null)).isNull();
	}
}
