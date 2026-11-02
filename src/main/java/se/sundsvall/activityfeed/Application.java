package se.sundsvall.activityfeed;

import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.dept44.configuration.SecurityConfiguration;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

import static org.springframework.boot.SpringApplication.run;

/**
 * dept44's SecurityConfiguration lets every request through. The API key protection in
 * {@link se.sundsvall.activityfeed.configuration.ApiKeySecurityConfiguration} replaces it.
 */
@ServiceApplication(exclude = SecurityConfiguration.class)
@ExcludeFromJacocoGeneratedCoverageReport
public class Application {

	public static void main(final String... args) {
		run(Application.class, args);
	}
}
