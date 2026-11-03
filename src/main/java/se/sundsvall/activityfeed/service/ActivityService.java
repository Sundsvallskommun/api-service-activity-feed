package se.sundsvall.activityfeed.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.activityfeed.api.model.CachadKundhandelse;
import se.sundsvall.activityfeed.api.model.NyKundhandelse;
import se.sundsvall.activityfeed.integration.db.ActivityRepository;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.activityfeed.service.mapper.ActivityMapper;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.activityfeed.integration.db.model.ActivityEntity.UNIQUE_ACTIVITY_ID;

/**
 * Maintains the activity cache. The business system sends in activities, the cache answers queries.
 */
@Service
public class ActivityService {

	private static final Logger LOG = LoggerFactory.getLogger(ActivityService.class);

	private final ActivityRepository repository;
	private final ProducerService producerService;

	public ActivityService(final ActivityRepository repository, final ProducerService producerService) {
		this.repository = repository;
		this.producerService = producerService;
	}

	/**
	 * Creates activities, or replaces those with the same kundhandelseId within the municipality.
	 *
	 * <p>
	 * The whole list is saved in one transaction. If two requests create the same new kundhandelseId at the same time,
	 * the unique database constraint stops duplicates. The request that loses gets 409 and can be sent again unchanged,
	 * since the operation is idempotent. Other database errors propagate, because sending the request again would not
	 * help.
	 *
	 * @param  municipalityId municipality
	 * @param  activities     activities to save
	 * @return                number of saved activities
	 */
	@Transactional
	public int save(final String municipalityId, final List<NyKundhandelse> activities) {
		final var producer = producerService.getProducer(municipalityId);
		final var prefix = producer.prefix() + ".";
		for (final var activity : activities) {
			if (activity.getPart().isEmpty()) {
				throw Problem.valueOf(BAD_REQUEST, "part måste innehålla kund, ärende eller båda (" + activity.getKundhandelseId() + ")");
			}
			if (!activity.getKundhandelseTyp().startsWith(prefix)) {
				throw Problem.valueOf(BAD_REQUEST, "kundhandelseTyp måste börja med producentprefixet " + prefix + " (" + activity.getKundhandelseId() + ")");
			}
		}
		try {
			for (final var activity : activities) {
				final var entity = repository.findByMunicipalityIdAndActivityId(municipalityId, activity.getKundhandelseId()).orElseGet(ActivityEntity::create);
				repository.save(ActivityMapper.toEntity(municipalityId, producer, producerService.standardVersion(), activity, entity));
			}
			repository.flush();
		} catch (final DataIntegrityViolationException e) {
			if (!isActivityIdCollision(e)) {
				throw e;
			}
			LOG.warn("Concurrent ingest of the same kundhandelseId: {}", e.getMostSpecificCause().getMessage());
			throw Problem.valueOf(CONFLICT, "Samtidig ändring av samma kundhändelse, skicka anropet igen");
		}
		return activities.size();
	}

	/**
	 * @param  e the database error
	 * @return   true if the error is a collision on the unique constraint for kundhandelseId within the municipality
	 */
	static boolean isActivityIdCollision(final DataIntegrityViolationException e) {
		return Stream.iterate(e, Objects::nonNull, Throwable::getCause)
			.filter(ConstraintViolationException.class::isInstance)
			.map(ConstraintViolationException.class::cast)
			.map(ConstraintViolationException::getConstraintName)
			.anyMatch(UNIQUE_ACTIVITY_ID::equalsIgnoreCase);
	}

	@Transactional(readOnly = true)
	public CachadKundhandelse read(final String municipalityId, final String activityId) {
		return ActivityMapper.toCachadKundhandelse(find(municipalityId, activityId));
	}

	@Transactional
	public void delete(final String municipalityId, final String activityId) {
		repository.delete(find(municipalityId, activityId));
	}

	@Transactional(readOnly = true)
	public long count(final String municipalityId) {
		producerService.validateProducer(municipalityId);
		return repository.countByMunicipalityId(municipalityId);
	}

	private ActivityEntity find(final String municipalityId, final String activityId) {
		producerService.validateProducer(municipalityId);
		return repository.findByMunicipalityIdAndActivityId(municipalityId, activityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Kundhändelsen " + activityId + " finns inte"));
	}
}
