package se.sundsvall.activityfeed.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import se.sundsvall.dept44.util.LogUtils;

/**
 * Reads the API key from the header and sets the security context if the key is valid. The rules in
 * {@link ApiKeySecurityConfiguration} decide whether the request may pass. A missing or invalid key gives 401 there.
 *
 * <p>
 * The key's id (e.g. admin-1, never the key) is put in the MDC as apiKeyId for the duration of the request, so log
 * lines
 * can be traced to the caller.
 */
class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	static final String MDC_API_KEY_ID = "apiKeyId";

	private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

	private final ApiKeyAuthenticator authenticator;
	private final String headerName;

	ApiKeyAuthenticationFilter(final ApiKeyAuthenticator authenticator, final String headerName) {
		this.authenticator = authenticator;
		this.headerName = headerName;
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final var key = request.getHeader(headerName);
		var authenticated = false;
		if (key != null) {
			final var authentication = authenticator.authenticate(key);
			if (authentication.isPresent()) {
				final var context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
				context.setAuthentication(authentication.get());
				SecurityContextHolder.getContextHolderStrategy().setContext(context);
				MDC.put(MDC_API_KEY_ID, authentication.get().getName());
				authenticated = true;
			} else {
				LOG.warn("Invalid API key in request {} {}", request.getMethod(), LogUtils.sanitizeForLogging(request.getRequestURI()));
			}
		}
		try {
			chain.doFilter(request, response);
		} finally {
			if (authenticated) {
				MDC.remove(MDC_API_KEY_ID);
				SecurityContextHolder.getContextHolderStrategy().clearContext();
			}
		}
	}
}
