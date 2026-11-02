package se.sundsvall.activityfeed.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Optional.ofNullable;

/**
 * Describes the API key in the OpenAPI specification so it can be entered in Swagger UI (the Authorize button).
 */
@Configuration
public class OpenApiSecurityConfiguration {

	public static final String API_KEY_SCHEME = "ApiKey";

	@Bean
	OpenApiCustomizer apiKeyOpenApiCustomizer(final ApiKeyProperties properties) {
		return openApi -> {
			final var components = ofNullable(openApi.getComponents()).orElseGet(Components::new);
			components.addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
				.type(SecurityScheme.Type.APIKEY)
				.in(SecurityScheme.In.HEADER)
				.name(properties.headerName())
				.description("API-nyckel. Fråge-nycklar (API_KEYS_QUERY) når /{municipalityId}/kundhandelseFragaSynkron, "
					+ "admin-nycklar (API_KEYS_ADMIN) når alla gränssnitt."));
			openApi.components(components);
			openApi.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
		};
	}
}
