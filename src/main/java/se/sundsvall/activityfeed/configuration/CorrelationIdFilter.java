package se.sundsvall.activityfeed.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import se.sundsvall.activityfeed.api.validation.Limits;

/**
 * Puts skv_client_correlation_id in the MDC (key skvClientCorrelationId) for the whole request, so log lines can be
 * traced to the call chain at Skatteverket. dept44's x-request-id stays in the MDC next to it. The controller validates
 * the format. This filter only lets valid values through, so the header cannot be used to tamper with the log.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "skv_client_correlation_id";
	public static final String MDC_KEY = "skvClientCorrelationId";

	private static final Pattern ALLOWED_FORMAT = Pattern.compile(Limits.CORRELATION_ID_PATTERN);

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final var id = request.getHeader(HEADER);
		final var set = id != null && ALLOWED_FORMAT.matcher(id).matches();
		if (set) {
			MDC.put(MDC_KEY, id);
		}
		try {
			chain.doFilter(request, response);
		} finally {
			if (set) {
				MDC.remove(MDC_KEY);
			}
		}
	}
}
