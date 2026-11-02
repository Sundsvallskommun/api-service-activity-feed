package se.sundsvall.activityfeed.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import se.sundsvall.dept44.problem.ProblemExceptionHandler;

/**
 * Errors raised while the request is matched against a controller (wrong method, wrong Content-Type), which therefore
 * never reach {@link ActivityQueryExceptionHandler}. For the query interface the response is {"message": "..."} as in
 * Skatteverket's API definition (405, 415). For every other path the error is passed on to dept44's
 * ProblemExceptionHandler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ActivityQueryRoutingExceptionHandler {

	private final ProblemExceptionHandler problemExceptionHandler;

	ActivityQueryRoutingExceptionHandler(final ProblemExceptionHandler problemExceptionHandler) {
		this.problemExceptionHandler = problemExceptionHandler;
	}

	@ExceptionHandler({
		HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class
	})
	ResponseEntity<?> handleRoutingException(final Exception e, final HttpServletRequest request, final WebRequest webRequest) throws Exception {
		if (ApiPaths.isActivityQuery(request)) {
			return ActivityQueryExceptionHandler.response(((org.springframework.web.ErrorResponse) e).getStatusCode(), null);
		}
		return problemExceptionHandler.handleException(e, webRequest);
	}
}
