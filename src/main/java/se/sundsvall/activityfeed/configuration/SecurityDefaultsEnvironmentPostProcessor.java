package se.sundsvall.activityfeed.configuration;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Security-critical settings that must take precedence over dept44's defaults.
 *
 * <p>
 * dept44-starter ships its defaults in classpath:/config/application.properties, which Spring Boot reads with higher
 * precedence than the service's own application.yml. Among other things those defaults turn on logging of request
 * payloads (with personal identity numbers in clear text), expose every actuator endpoint and show health details to
 * everyone. This class puts the service's values above all configuration files but below
 * environment variables, system properties and command line arguments, so they can still be changed explicitly when
 * troubleshooting.
 */
public class SecurityDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String PROPERTY_SOURCE_NAME = "activityFeedSecurityDefaults";

	static final Map<String, Object> SETTINGS = Map.of(
		// Request payloads are not logged.
		"logging.level.se.sundsvall.dept44.payload", "OFF",
		// Only health and info are exposed.
		"management.endpoints.web.exposure.include", "health,info",
		// Health details (database, disk etc.) are not shown.
		"management.endpoint.health.show-details", "never",
		"management.endpoint.health.show-components", "never");

	@Override
	public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
		final var propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, SETTINGS);
		final var sources = environment.getPropertySources();
		if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
			sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
		} else {
			sources.addFirst(propertySource);
		}
	}

	/**
	 * After the configuration files have been read.
	 */
	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER + 1;
	}
}
