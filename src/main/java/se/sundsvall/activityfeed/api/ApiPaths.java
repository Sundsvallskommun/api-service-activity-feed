package se.sundsvall.activityfeed.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.function.Predicate.not;

/**
 * Paths to the service's interfaces. All of them sit under the municipality id (dept44 convention), so one instance can
 * be the producer for several municipalities. The forwarding service is configured with the base URL
 * https://host/{municipalityId}.
 */
public final class ApiPaths {

	/** The query interface as defined by Skatteverket's API definition. */
	public static final String ACTIVITY_QUERY = "/{municipalityId}/kundhandelseFragaSynkron";

	/** Maintenance of the activity cache. */
	public static final String ACTIVITIES = "/{municipalityId}/kundhandelser";

	private static final Pattern ACTIVITY_QUERY_PATTERN = Pattern.compile("^/[^/]+/kundhandelseFragaSynkron/?$");

	private ApiPaths() {}

	/**
	 * The query interface answers with the error format in Skatteverket's API definition ({"message": "..."}) instead of
	 * RFC 9457, also for errors raised before the request reaches the controller (401, 403, 405, 415).
	 *
	 * @param  request the request
	 * @return         true if the request is for the query interface
	 */
	public static boolean isActivityQuery(final HttpServletRequest request) {
		final var uri = request.getRequestURI();
		final var path = Optional.ofNullable(request.getContextPath())
			.filter(not(String::isEmpty))
			.filter(uri::startsWith)
			.map(contextPath -> uri.substring(contextPath.length()))
			.orElse(uri);
		return ACTIVITY_QUERY_PATTERN.matcher(path).matches();
	}
}
