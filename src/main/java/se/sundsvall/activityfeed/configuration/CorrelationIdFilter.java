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

/**
 * Puts the header skv_client_correlation_id in the MDC (key skvClientCorrelationId) for the whole request. The caller
 * of the query interface is Skatteverket's forwarding service, which sends the header as defined in Skatteverket's API
 * definition for Mina ärenden kundhändelser. With it in the MDC, every log line of a call can be found by the id
 * Skatteverket knows the call by. dept44's x-request-id stays in the MDC next to it. The filter runs on every request,
 * but only the query interface declares the header. On the cache interface a header that a caller sends anyway still
 * goes through the same format check before it reaches the MDC.
 *
 * <p>
 * The controller validates the format. This filter only lets valid values through, so the header cannot be used to
 * tamper with the log.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "skv_client_correlation_id";
	public static final String MDC_KEY = "skvClientCorrelationId";

	/** Longest allowed value. */
	public static final int MAX_LENGTH = 200;

	/**
	 * Printable ASCII characters (spaces included), at least one visible character and at most {@link #MAX_LENGTH}. The
	 * API definition allows any format. The service description recommends a UUID. The controller validates the header
	 * with the same format.
	 */
	public static final String FORMAT = "^(?=.*[\\x21-\\x7E])[\\x20-\\x7E]{1," + MAX_LENGTH + "}$";

	private static final Pattern ALLOWED_FORMAT = Pattern.compile(FORMAT);

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
