package se.sundsvall.activityfeed.service;

import org.springframework.stereotype.Service;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProducerService {

	private final ActivityFeedProperties properties;

	public ProducerService(final ActivityFeedProperties properties) {
		this.properties = properties;
	}

	/**
	 * @param  municipalityId                               municipality id from the path
	 * @return                                              the producer for the municipality
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem 404 if the municipality is not configured
	 */
	public Producer getProducer(final String municipalityId) {
		return properties.producer(municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Kommun " + municipalityId + " är inte konfigurerad som producent"));
	}

	public String standardVersion() {
		return properties.standardVersion();
	}
}
