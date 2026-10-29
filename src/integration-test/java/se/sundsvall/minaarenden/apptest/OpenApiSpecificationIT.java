package se.sundsvall.minaarenden.apptest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.dept44.util.ResourceUtils;
import se.sundsvall.minaarenden.Application;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static java.nio.file.Files.writeString;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * Jämför tjänstens OpenAPI-specifikation med den incheckade (src/integration-test/resources/api/openapi.yaml).
 * Ändras API:et skrivs den nya specifikationen till target/api.yaml; granska och kopiera den till den incheckade filen.
 */
@ActiveProfiles("it")
@AutoConfigureTestRestTemplate
@SpringBootTest(
	webEnvironment = WebEnvironment.RANDOM_PORT,
	classes = Application.class,
	properties = {
		"spring.main.banner-mode=off",
		"logging.level.se.sundsvall.dept44.payload=OFF"
	})
class OpenApiSpecificationIT {

	private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

	@Value("classpath:/api/openapi.yaml")
	private Resource openApiResource;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void compareOpenApiSpecifications() throws IOException {
		final var existingOpenApiSpecification = ResourceUtils.asString(openApiResource);
		final var currentOpenApiSpecification = restTemplate.getForObject("/api-docs.yaml", String.class);

		writeString(Path.of("target/api.yaml"), currentOpenApiSpecification);

		assertThatJson(toJson(currentOpenApiSpecification))
			.withOptions(List.of(IGNORING_ARRAY_ORDER))
			.whenIgnoringPaths("servers")
			.isEqualTo(toJson(existingOpenApiSpecification));
	}

	private static String toJson(final String yaml) {
		return YAML_MAPPER.readTree(yaml).toString();
	}
}
