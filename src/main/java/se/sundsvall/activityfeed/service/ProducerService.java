package se.sundsvall.activityfeed.service;

import org.springframework.stereotype.Service;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties;
import se.sundsvall.activityfeed.configuration.ActivityFeedProperties.Producer;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

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
		return properties.producer(municipalityId).orElseThrow(() -> notConfigured(municipalityId));
	}

	/**
	 * For calls that only need to know that the municipality is a producer, not which producer it is.
	 *
	 * @param  municipalityId                               municipality id from the path
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem 404 if the municipality is not configured
	 */
	public void validateProducer(final String municipalityId) {
		if (properties.producer(municipalityId).isEmpty()) {
			throw notConfigured(municipalityId);
		}
	}

	public String standardVersion() {
		return properties.standardVersion();
	}

	private static ThrowableProblem notConfigured(final String municipalityId) {
		return Problem.valueOf(NOT_FOUND, "Kommun " + municipalityId + " är inte konfigurerad som producent");
	}
}
