package se.sundsvall.activityfeed.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.activityfeed.api.model.Behandling;
import se.sundsvall.activityfeed.api.model.Delfraga;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaRequest;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaResponse;
import se.sundsvall.activityfeed.api.model.KundhandelserForPart;
import se.sundsvall.activityfeed.api.model.Metadata;
import se.sundsvall.activityfeed.api.model.Paginering;
import se.sundsvall.activityfeed.integration.db.ActivityRepository;
import se.sundsvall.activityfeed.integration.db.OffsetPageable;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.activityfeed.service.mapper.ActivityMapper;
import se.sundsvall.activityfeed.util.DateTimeUtil;

import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.forParty;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.sortedBy;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withActivityTypes;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withAnyTag;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withMunicipalityId;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withOccurredFrom;
import static se.sundsvall.activityfeed.integration.db.specification.ActivitySpecification.withOccurredTo;

/**
 * Answers a synchronous query from the activity cache. As the only producer, the service always answers 200 with one
 * delfraga per party with status OK. The forwarding service (vidareförmedlingstjänsten) sets 206 if a producer fails.
 *
 * <p>
 * Filtering, sorting and pagination happen in the database. Only the requested page is loaded into memory, and
 * totaltAntalKundhandelser comes from a separate COUNT query when needed.
 */
@Service
public class ActivityQueryService {

	static final String STATUS_OK = "OK";

	private final ActivityRepository repository;
	private final ProducerService producerService;

	public ActivityQueryService(final ActivityRepository repository, final ProducerService producerService) {
		this.repository = repository;
		this.producerService = producerService;
	}

	@Transactional(readOnly = true)
	public KundhandelseFragaResponse answer(final String municipalityId, final KundhandelseFragaRequest request, final String acceptLanguage) {
		final var producer = producerService.getProducer(municipalityId);
		final var query = request.getFraga();

		final var processing = Optional.ofNullable(query.getBehandling());
		final var sorting = processing.map(Behandling::getSortering).orElse(null);
		final var pagination = processing.map(Behandling::getPaginering).orElse(null);
		final var from = DateTimeUtil.startOfDay(query.getStartDatum());
		final var to = DateTimeUtil.endOfDay(query.getSlutDatum());

		final var perParty = new ArrayList<KundhandelserForPart>();
		final var subQueries = new ArrayList<Delfraga>();
		for (final var party : query.getParter()) {
			final var specification = Specification.allOf(
				withMunicipalityId(municipalityId),
				forParty(party.getKund(), party.getArende()),
				withActivityTypes(query.getKundhandelseTyper()),
				withAnyTag(query.getTaggar()),
				withOccurredFrom(from),
				withOccurredTo(to),
				sortedBy(sorting));

			perParty.add(search(specification, pagination).withPart(party));
			subQueries.add(Delfraga.create()
				.withProducent(producer.name())
				.withPart(party)
				.withStatus(STATUS_OK)
				.withHttpCode(200));
		}

		return KundhandelseFragaResponse.create()
			.withKundhandelser(perParty)
			.withMetadata(Metadata.create()
				.withParter(query.getParter())
				.withKundhandelseTyper(query.getKundhandelseTyper())
				.withTaggar(query.getTaggar())
				.withStartDatum(query.getStartDatum())
				.withSlutDatum(query.getSlutDatum())
				.withOnskatSprak(acceptLanguage)
				.withBehandling(query.getBehandling()))
			.withDelfragor(subQueries);
	}

	private KundhandelserForPart search(final Specification<ActivityEntity> specification, final Paginering pagination) {
		final List<ActivityEntity> hits;
		final long total;
		if (pagination == null) {
			hits = repository.findAll(specification);
			total = hits.size();
		} else {
			// Without a limit, all activities from offset to the end are returned (spec).
			final var limit = Optional.ofNullable(pagination.getLimit()).orElse(Integer.MAX_VALUE);
			final var page = repository.findAll(specification, new OffsetPageable(pagination.getOffset(), limit));
			hits = page.getContent();
			total = page.getTotalElements();
		}
		return KundhandelserForPart.create()
			.withTotaltAntalKundhandelser(total)
			.withKundhandelserForPart(hits.stream().map(ActivityMapper::toKundhandelse).toList());
	}

}
