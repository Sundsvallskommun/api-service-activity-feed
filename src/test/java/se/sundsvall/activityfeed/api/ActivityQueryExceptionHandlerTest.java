package se.sundsvall.activityfeed.api;

import java.time.DateTimeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import se.sundsvall.activityfeed.api.model.ErrorResponse;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;

class ActivityQueryExceptionHandlerTest {

	private final ActivityQueryExceptionHandler handler = new ActivityQueryExceptionHandler();

	@Test
	void problemWithServerErrorHidesDetails() {
		final var response = handler.handleProblem(Problem.valueOf(BAD_GATEWAY, "intern detalj"));

		assertThat(response.getStatusCode()).isEqualTo(BAD_GATEWAY);
		assertThat(response.getBody()).isEqualTo(ErrorResponse.create().withMessage("Bad Gateway"));
	}

	@Test
	void problemWithoutStatusBecomes500() {
		final var response = handler.handleProblem(new ThrowableProblem(null, "title", (HttpStatus) null, "detail", null));

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
	}

	@Test
	void problemWithClientError() {
		assertThat(handler.handleProblem(Problem.valueOf(CONFLICT, "samtidig ändring")).getBody().getMessage()).isEqualTo("Conflict: samtidig ändring");
	}

	@Test
	void otherErrors() {
		assertThat(handler.handleDateTime(new DateTimeException("x")).getBody().getMessage()).isEqualTo("Bad request: ogiltigt datum eller ogiltig tidpunkt");
		assertThat(handler.handleIllegalArgument(new IllegalArgumentException("x")).getBody().getMessage()).isEqualTo("Bad request: ogiltigt innehåll");
		assertThat(handler.handleException(new RuntimeException("x")).getStatusCode().value()).isEqualTo(500);
		assertThat(handler.handleException(new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE)).getBody().getMessage()).isEqualTo("Not Acceptable");
		assertThat(handler.handleException(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).getBody().getMessage()).isEqualTo("Internal server error");
	}

	@Test
	void fieldName() {
		assertThat(ActivityQueryExceptionHandler.fieldName("kundhandelseFragaSynkron.correlationId")).isEqualTo("skv_client_correlation_id");
		assertThat(ActivityQueryExceptionHandler.fieldName("kundhandelseFragaSynkron.municipalityId")).isEqualTo("municipalityId");
		assertThat(ActivityQueryExceptionHandler.fieldName("field")).isEqualTo("field");
	}

	@Test
	void titles() {
		assertThat(ActivityQueryExceptionHandler.title(HttpStatus.UNAUTHORIZED)).isEqualTo("Unauthorized");
		assertThat(ActivityQueryExceptionHandler.title(HttpStatus.FORBIDDEN)).isEqualTo("Forbidden");
		assertThat(ActivityQueryExceptionHandler.title(HttpStatus.I_AM_A_TEAPOT)).isEqualTo("I'm a teapot");
		assertThat(ActivityQueryExceptionHandler.title(HttpStatusCode.valueOf(599))).isEqualTo("Error");
	}
}
