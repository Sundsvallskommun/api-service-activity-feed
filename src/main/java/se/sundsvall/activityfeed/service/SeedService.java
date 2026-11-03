package se.sundsvall.activityfeed.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.activityfeed.integration.db.ActivityRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Logs at startup how many activities each municipality has, so that after a redeploy the log shows right away that
 * the data is still there. If activity-feed.seed is true, it also loads sample activities for municipalities that have
 * none at all (development and demo only). Existing data is never touched.
 */
@Component
public class SeedService implements ApplicationRunner {

	/** Prefix the sample file is written with. Replaced by the municipality's prefix on load. */
	static final String SAMPLE_PREFIX = "REFKOM";
	static final String SAMPLE_FILE = "sample-activities.json";

	private static final Logger LOG = LoggerFactory.getLogger(SeedService.class);

	private final ActivityFeedProperties properties;
	private final ActivityService service;
	private final ActivityRepository repository;
	private final JsonMapper jsonMapper;

	public SeedService(final ActivityFeedProperties properties, final ActivityService service, final ActivityRepository repository, final JsonMapper jsonMapper) {
		this.properties = properties;
		this.service = service;
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void run(final ApplicationArguments args) throws IOException {
		for (final var producer : properties.producers()) {
			final var count = repository.countByMunicipalityId(producer.municipalityId());
			LOG.info("The activity cache contains {} activities for municipality {} ({})", count, producer.municipalityId(), producer.name());
			if (properties.seed() && count == 0) {
				loadSamples(producer);
			}
		}
	}

	private void loadSamples(final Producer producer) throws IOException {
		try (var in = new ClassPathResource(SAMPLE_FILE).getInputStream()) {
			final List<NyKundhandelse> samples = jsonMapper.readValue(in, new TypeReference<>() {});
			samples.forEach(sample -> replacePrefix(sample, producer.prefix()));
			final var count = service.save(producer.municipalityId(), samples);
			LOG.info("Loaded {} sample activities for municipality {} ({})", count, producer.municipalityId(), producer.name());
		}
	}

	/**
	 * Replaces the producer prefix in the fields where it occurs (kundhandelseId and kundhandelseTyp), nothing else.
	 */
	static NyKundhandelse replacePrefix(final NyKundhandelse activity, final String prefix) {
		return activity
			.withKundhandelseId(replaceStart(activity.getKundhandelseId(), SAMPLE_PREFIX + "-", prefix + "-"))
			.withKundhandelseTyp(replaceStart(activity.getKundhandelseTyp(), SAMPLE_PREFIX + ".", prefix + "."));
	}

	private static String replaceStart(final String value, final String from, final String to) {
		return Optional.ofNullable(value)
			.filter(v -> v.startsWith(from))
			.map(v -> to + v.substring(from.length()))
			.orElse(value);
	}
}
