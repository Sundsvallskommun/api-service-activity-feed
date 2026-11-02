package se.sundsvall.activityfeed.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Timestamps per RFC 3339 in responses, lenient on input. Date boundaries are interpreted in Swedish time.
 */
public final class DateTimeUtil {

	public static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

	private static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private DateTimeUtil() {}

	/**
	 * @param  value RFC 3339 (2026-09-01T10:15:00+02:00) or local Swedish time (2026-09-01T10:15:00 or 2026-09-01
	 *               10:15:00)
	 * @return       the point in time
	 */
	public static Instant parse(final String value) {
		try {
			return OffsetDateTime.parse(value).toInstant();
		} catch (final DateTimeParseException ignored) {
			// try the local formats
		}
		try {
			return LocalDateTime.parse(value).atZone(ZONE).toInstant();
		} catch (final DateTimeParseException ignored) {
			// try the next one
		}
		return LocalDateTime.parse(value, LOCAL_FORMAT).atZone(ZONE).toInstant();
	}

	public static String format(final Instant instant) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(ZONE).toOffsetDateTime());
	}

	/**
	 * @return the date at 00:00 Swedish time, or null
	 */
	public static Instant startOfDay(final String date) {
		return Optional.ofNullable(date)
			.map(d -> LocalDate.parse(d).atStartOfDay(ZONE).toInstant())
			.orElse(null);
	}

	/**
	 * @return the date at 23:59:59.999 Swedish time, or null
	 */
	public static Instant endOfDay(final String date) {
		return Optional.ofNullable(date)
			.map(d -> LocalDate.parse(d).plusDays(1).atStartOfDay(ZONE).toInstant().minusMillis(1))
			.orElse(null);
	}
}
