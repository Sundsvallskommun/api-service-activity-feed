package se.sundsvall.activityfeed.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

/**
 * Producer configuration per municipality. Every municipality that answers queries or receives activities must be in
 * the producers list. Requests for other municipality ids get 404.
 *
 * @param standardVersion version of the Mina ärenden standard that the producer implements, e.g. 6.1
 * @param seed            load sample activities at startup for municipalities that have no activities (development and
 *                        demo only)
 * @param producers       producers, one per municipality
 */
@Validated
@ConfigurationProperties(prefix = "activity-feed")
public record ActivityFeedProperties(
	@NotBlank @Pattern(regexp = "^\\d+(?:\\.\\d+)*$") @DefaultValue("6.1") String standardVersion,
	@DefaultValue("false") boolean seed,
	@NotEmpty List<@Valid Producer> producers) {

	public ActivityFeedProperties {
		producers = Optional.ofNullable(producers).map(List::copyOf).orElse(List.of());
		final var municipalityIds = new HashSet<String>();
		producers.stream()
			.map(Producer::municipalityId)
			.filter(id -> !municipalityIds.add(id))
			.findFirst()
			.ifPresent(id -> {
				throw new IllegalStateException("Municipality id " + id + " occurs more than once in activity-feed.producers");
			});
	}

	/**
	 * @param  municipalityId municipality id, e.g. 2281
	 * @return                the producer for the municipality, if configured
	 */
	public Optional<Producer> producer(final String municipalityId) {
		return producers.stream()
			.filter(producer -> producer.municipalityId().equals(municipalityId))
			.findFirst();
	}

	/**
	 * @param municipalityId municipality id (four digits), the first part of the path
	 * @param name           the producer's name in responses (delfragor.producent and kundhandelse.producent)
	 * @param prefix         producer prefix, the first part of activity types (uppercase A-Z)
	 */
	public record Producer(
		@ValidMunicipalityId String municipalityId,
		@NotBlank String name,
		@NotBlank @Pattern(regexp = "^[A-Z]+$", message = "must consist of uppercase letters A-Z, e.g. REFKOM") String prefix) {
	}
}
