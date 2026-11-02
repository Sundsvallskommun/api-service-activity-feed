package se.sundsvall.activityfeed.configuration;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityDefaultsEnvironmentPostProcessorTest {

	private final SecurityDefaultsEnvironmentPostProcessor postProcessor = new SecurityDefaultsEnvironmentPostProcessor();

	@Test
	void takesPrecedenceOverConfigFilesButNotEnvironmentVariables() {
		final var environment = new StandardEnvironment();
		environment.getPropertySources().addLast(new MapPropertySource("Config resource 'class path resource [config/application.properties]'", Map.of(
			"logging.level.se.sundsvall.dept44.payload", "TRACE",
			"management.endpoints.web.exposure.include", "*",
			"management.endpoint.health.show-details", "always")));

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getProperty("logging.level.se.sundsvall.dept44.payload")).isEqualTo("OFF");
		assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health,info,flyway");
		assertThat(environment.getProperty("management.endpoint.health.show-details")).isEqualTo("when-authorized");
		assertThat(environment.getProperty("management.endpoint.health.roles")).isEqualTo("ADMIN");

		final var sources = environment.getPropertySources().stream().map(PropertySource::getName).toList();
		assertThat(sources.indexOf(SecurityDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
			.isEqualTo(sources.indexOf(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) + 1);
	}

	@Test
	void withoutSystemEnvironmentSettingsGoFirst() {
		final var environment = new MockEnvironment();

		postProcessor.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getPropertySources().iterator().next().getName()).isEqualTo(SecurityDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);
	}

	@Test
	void runsAfterConfigurationIsLoaded() {
		assertThat(postProcessor.getOrder()).isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER);
	}
}
