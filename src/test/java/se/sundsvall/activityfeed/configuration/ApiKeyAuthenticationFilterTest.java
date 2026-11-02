package se.sundsvall.activityfeed.configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticationFilterTest {

	private static final String KEY = "n".repeat(40);

	private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
		new ApiKeyAuthenticator(new ApiKeyProperties("X-API-Key", List.of(KEY), List.of())), "X-API-Key");

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validKeySetsContextAndMdcDuringRequest() throws Exception {
		final var authentication = new AtomicReference<Authentication>();
		final var mdc = new AtomicReference<String>();

		filter.doFilter(request(KEY), new MockHttpServletResponse(), (_, _) -> {
			authentication.set(SecurityContextHolder.getContext().getAuthentication());
			mdc.set(MDC.get(ApiKeyAuthenticationFilter.MDC_API_KEY_ID));
		});

		assertThat(authentication.get().getName()).isEqualTo("query-1");
		assertThat(mdc.get()).isEqualTo("query-1");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(MDC.get(ApiKeyAuthenticationFilter.MDC_API_KEY_ID)).isNull();
	}

	@Test
	void invalidOrMissingKeyLeavesContextEmpty() throws Exception {
		for (final var key : new String[] {
			"wrong", null
		}) {
			final var authentication = new AtomicReference<Authentication>();
			filter.doFilter(request(key), new MockHttpServletResponse(), (_, _) -> authentication.set(SecurityContextHolder.getContext().getAuthentication()));
			assertThat(authentication.get()).isNull();
		}
	}

	private static MockHttpServletRequest request(final String key) {
		final var request = new MockHttpServletRequest("GET", "/2281/kundhandelser");
		if (key != null) {
			request.addHeader("X-API-Key", key);
		}
		return request;
	}
}
