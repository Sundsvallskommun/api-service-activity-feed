package se.sundsvall.activityfeed.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityQueryRoutingExceptionHandlerTest {

	@ParameterizedTest
	@CsvSource(value = {
		"'', /2281/kundhandelseFragaSynkron, true",
		"'', /2281/kundhandelseFragaSynkron/, true",
		"/api, /api/2281/kundhandelseFragaSynkron, true",
		"'', /2281/kundhandelser, false",
		"'', /kundhandelseFragaSynkron, false",
		"'', /2281/kundhandelseFragaSynkron/x, false"
	})
	void isActivityQuery(final String contextPath, final String uri, final boolean expected) {
		final var request = new MockHttpServletRequest("POST", uri);
		request.setContextPath(contextPath);

		assertThat(ActivityQueryRoutingExceptionHandler.isActivityQuery(request)).isEqualTo(expected);
	}
}
