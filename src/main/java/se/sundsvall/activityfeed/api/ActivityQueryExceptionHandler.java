package se.sundsvall.activityfeed.api;

import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.sundsvall.activityfeed.api.model.ErrorResponse;
import se.sundsvall.activityfeed.configuration.CorrelationIdFilter;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.DatabindException;

import static java.util.function.Predicate.not;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Error responses for the query interface as defined by Skatteverket's API definition: JSON with a single field,
 * message. The schema allows no other fields, so this is not RFC 9457 like the rest of the service. The messages are
 * our own stable texts that point out the field. Technical details from the underlying exception stay in the server
 * log.
 *
 * <p>
 * Applies only to {@link ActivityQueryResource} and runs before dept44's ProblemExceptionHandler. Errors raised before
 * the request reaches the controller are handled by {@link ActivityQueryRoutingExceptionHandler}.
 */
@RestControllerAdvice(assignableTypes = ActivityQueryResource.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActivityQueryExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ActivityQueryExceptionHandler.class);

	/** Parameters in {@link ActivityQueryResource} that correspond to headers. */
	private static final Map<String, String> PARAMETER_TO_FIELD = Map.of("correlationId", CorrelationIdFilter.HEADER);

	@ExceptionHandler(ThrowableProblem.class)
	ResponseEntity<ErrorResponse> handleProblem(final ThrowableProblem problem) {
		final var status = Optional.ofNullable(problem.getStatus()).orElse(INTERNAL_SERVER_ERROR);
		if (status.is5xxServerError()) {
			LOG.error("Error while answering query", problem);
			return response(status, null);
		}
		return response(status, problem.getDetail());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleNotReadable(final HttpMessageNotReadableException e) {
		final var message = Optional.ofNullable(jsonFieldPath(e))
			.map(field -> "fältet " + field + " har fel typ eller format")
			.orElse("meddelandet kan inte tolkas som JSON");
		return badRequest(message, e);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
		final var details = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		return badRequest(details.isEmpty() ? "ogiltigt innehåll" : details, e);
	}

	/**
	 * Validation of method parameters (@Validated): municipalityId and the header skv_client_correlation_id. The path
	 * starts with the method name, which is removed. Parameters that correspond to headers get the header's name.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(final ConstraintViolationException e) {
		final var details = e.getConstraintViolations().stream()
			.map(violation -> fieldName(violation.getPropertyPath().toString()) + ": " + violation.getMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		return badRequest(details.isEmpty() ? "ogiltigt innehåll" : details, e);
	}

	static String fieldName(final String path) {
		final var withoutMethod = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
		return PARAMETER_TO_FIELD.getOrDefault(withoutMethod, withoutMethod);
	}

	@ExceptionHandler(DateTimeException.class)
	ResponseEntity<ErrorResponse> handleDateTime(final DateTimeException e) {
		return badRequest("ogiltigt datum eller ogiltig tidpunkt", e);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException e) {
		return badRequest("ogiltigt innehåll", e);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	ResponseEntity<ErrorResponse> handleMissingHeader(final MissingRequestHeaderException e) {
		return badRequest("headern " + e.getHeaderName() + " saknas", e);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleException(final Exception e) {
		if (e instanceof final org.springframework.web.ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
			LOG.debug("Rejected request ({})", errorResponse.getStatusCode().value(), e);
			return response(errorResponse.getStatusCode(), null);
		}
		LOG.error("Unexpected error while answering query", e);
		return response(INTERNAL_SERVER_ERROR, null);
	}

	static ResponseEntity<ErrorResponse> response(final HttpStatusCode status, final String details) {
		final var title = title(status);
		final var message = Optional.ofNullable(details)
			.filter(not(String::isBlank))
			.map(detail -> title + ": " + detail)
			.orElse(title);
		return ResponseEntity.status(status)
			.contentType(APPLICATION_JSON)
			.body(ErrorResponse.create().withMessage(message));
	}

	/**
	 * The same texts as in Skatteverket's API definition.
	 */
	static String title(final HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "Bad request";
			case 401 -> "Unauthorized";
			case 403 -> "Forbidden";
			case 404 -> "Not found";
			case 405 -> "Method not allowed";
			case 409 -> "Conflict";
			case 415 -> "Unsupported media type";
			case 500 -> "Internal server error";
			default -> Optional.ofNullable(HttpStatus.resolve(status.value())).map(HttpStatus::getReasonPhrase).orElse("Error");
		};
	}

	private static ResponseEntity<ErrorResponse> badRequest(final String details, final Exception e) {
		LOG.debug("Rejected request (400): {}", details, e);
		return response(BAD_REQUEST, details);
	}

	/**
	 * The field's path in the JSON (e.g. fraga.parter[0].kund) without class names, if the error comes from Jackson.
	 */
	static String jsonFieldPath(final HttpMessageNotReadableException e) {
		if (!(e.getCause() instanceof final DatabindException databindException) || databindException.getPath().isEmpty()) {
			return null;
		}
		final var path = new StringBuilder();
		for (final var reference : databindException.getPath()) {
			if (reference.getPropertyName() != null) {
				if (!path.isEmpty()) {
					path.append('.');
				}
				path.append(reference.getPropertyName());
			} else if (reference.getIndex() >= 0) {
				path.append('[').append(reference.getIndex()).append(']');
			}
		}
		return path.isEmpty() ? null : path.toString();
	}
}
