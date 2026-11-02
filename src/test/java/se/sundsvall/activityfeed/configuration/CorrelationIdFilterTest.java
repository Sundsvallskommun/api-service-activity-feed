package se.sundsvall.activityfeed.configuration;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void validIdIsPutInMdcDuringRequest() throws Exception {
		assertThat(mdcDuringRequest("0002aa29-49f2-4baf-be51-c7c39c9824b4")).isEqualTo("0002aa29-49f2-4baf-be51-c7c39c9824b4");
		assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void invalidIdIsNotPutInMdc() throws Exception {
		assertThat(mdcDuringRequest("line\nbreak")).isNull();
		assertThat(mdcDuringRequest("   ")).isNull();
		assertThat(mdcDuringRequest("x".repeat(201))).isNull();
		assertThat(mdcDuringRequest(null)).isNull();
	}

	private String mdcDuringRequest(final String id) throws Exception {
		final var request = new MockHttpServletRequest("POST", "/2281/kundhandelseFragaSynkron");
		if (id != null) {
			request.addHeader(CorrelationIdFilter.HEADER, id);
		}
		final var value = new AtomicReference<String>();
		filter.doFilter(request, new MockHttpServletResponse(), (_, _) -> value.set(MDC.get(CorrelationIdFilter.MDC_KEY)));
		return value.get();
	}
}
