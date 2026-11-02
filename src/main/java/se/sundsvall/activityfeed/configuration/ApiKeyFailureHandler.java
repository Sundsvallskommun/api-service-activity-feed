package se.sundsvall.activityfeed.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import se.sundsvall.activityfeed.api.ApiPaths;
import se.sundsvall.activityfeed.api.model.ErrorResponse;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ProblemResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

/**
 * Response when the API key is missing or invalid (401) or does not grant access to the interface (403). The query
 * interface answers in Skatteverket's format {"message": "..."}, the other interfaces with RFC 9457 like the rest of
 * dept44.
 */
@Component
class ApiKeyFailureHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;
	private final String headerName;

	ApiKeyFailureHandler(final JsonMapper jsonMapper, final ApiKeyProperties properties) {
		this.jsonMapper = jsonMapper;
		this.headerName = properties.headerName();
	}

	@Override
	public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException) throws IOException {
		response.setHeader("WWW-Authenticate", "ApiKey header=\"" + headerName + "\"");
		write(request, response, UNAUTHORIZED, "API-nyckel saknas eller är ogiltig (header " + headerName + ")");
	}

	@Override
	public void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException accessDeniedException) throws IOException {
		write(request, response, FORBIDDEN, "API-nyckeln ger inte behörighet till den här resursen");
	}

	private void write(final HttpServletRequest request, final HttpServletResponse response, final HttpStatus status, final String detail) throws IOException {
		response.setStatus(status.value());
		if (ApiPaths.isActivityQuery(request)) {
			response.setContentType(APPLICATION_JSON_VALUE);
			jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.create().withMessage(status == UNAUTHORIZED ? "Unauthorized" : "Forbidden"));
		} else {
			response.setContentType(APPLICATION_PROBLEM_JSON_VALUE);
			jsonMapper.writeValue(response.getOutputStream(), new ProblemResponse(Problem.valueOf(status, detail)));
		}
	}
}
