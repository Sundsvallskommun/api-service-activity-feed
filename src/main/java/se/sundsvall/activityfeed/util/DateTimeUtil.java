package se.sundsvall.activityfeed.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Timestamps per RFC 3339 in responses, always with the Swedish offset. Date boundaries are interpreted in Swedish
 * time.
 */
public final class DateTimeUtil {

	public static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

	private DateTimeUtil() {}

	/**
	 * @param  dateTime a point in time with any offset
	 * @return          RFC 3339 with the Swedish offset at that point in time
	 */
	public static String format(final OffsetDateTime dateTime) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.atZoneSameInstant(ZONE).toOffsetDateTime());
	}

	/**
	 * @return the date at 00:00 Swedish time, or null
	 */
	public static OffsetDateTime startOfDay(final String date) {
		return Optional.ofNullable(date)
			.map(d -> LocalDate.parse(d).atStartOfDay(ZONE).toOffsetDateTime())
			.orElse(null);
	}

	/**
	 * @return the date at 23:59:59.999 Swedish time, or null
	 */
	public static OffsetDateTime endOfDay(final String date) {
		return Optional.ofNullable(date)
			.map(d -> LocalDate.parse(d).plusDays(1).atStartOfDay(ZONE).toOffsetDateTime().minus(1, MILLIS))
			.orElse(null);
	}
}
