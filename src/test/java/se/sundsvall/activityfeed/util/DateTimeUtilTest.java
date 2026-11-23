package se.sundsvall.activityfeed.util;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilTest {

	@Test
	void formatWithSwedishOffset() {
		assertThat(DateTimeUtil.format(OffsetDateTime.parse("2026-09-15T10:00:00Z"))).isEqualTo("2026-09-15T12:00:00+02:00");
		assertThat(DateTimeUtil.format(OffsetDateTime.parse("2026-12-15T11:00:00Z"))).isEqualTo("2026-12-15T12:00:00+01:00");
	}

	@Test
	void sameLocalTimeAtSwitchToWinterTimeKeepsTheOffset() {
		// 02:00-03:00 on 25 October 2026 occurs twice in Swedish time. The offset tells the two apart.
		final var summerTime = OffsetDateTime.parse("2026-10-25T02:30:00+02:00");
		final var winterTime = OffsetDateTime.parse("2026-10-25T02:30:00+01:00");

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
