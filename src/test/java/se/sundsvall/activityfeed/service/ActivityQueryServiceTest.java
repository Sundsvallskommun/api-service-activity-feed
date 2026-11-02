package se.sundsvall.activityfeed.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.activityfeed.api.model.Behandling;
import se.sundsvall.activityfeed.api.model.KundhandelseFragaRequest;
import se.sundsvall.activityfeed.api.model.Paginering;
import se.sundsvall.activityfeed.api.model.Part;
import se.sundsvall.activityfeed.api.model.Sortering;
import se.sundsvall.activityfeed.integration.db.ActivityRepository;
import se.sundsvall.activityfeed.integration.db.OffsetPageable;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.activityfeed.service.TestdataFactory.MUNICIPALITY_ID;
import static se.sundsvall.activityfeed.service.TestdataFactory.PRODUCER;
import static se.sundsvall.activityfeed.service.TestdataFactory.entity;
import static se.sundsvall.activityfeed.service.TestdataFactory.fraga;
import static se.sundsvall.activityfeed.service.TestdataFactory.kund;

@ExtendWith(MockitoExtension.class)
class ActivityQueryServiceTest {

	@Mock
	private ActivityRepository repository;

	@Mock
	private ProducerService producerService;

	@InjectMocks
	private ActivityQueryService service;

	@Test
	@SuppressWarnings("unchecked")
	void answerWithoutPagination() {
		final var request = fraga();
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity("TESTKOP-1"), entity("TESTKOP-2")));

		final var response = service.answer(MUNICIPALITY_ID, request, "sv");

		assertThat(response.getKundhandelser()).hasSize(1).first().satisfies(perParty -> {
			assertThat(perParty.getPart()).isEqualTo(request.getFraga().getParter().getFirst());
			assertThat(perParty.getTotaltAntalKundhandelser()).isEqualTo(2);
			assertThat(perParty.getKundhandelserForPart()).extracting(k -> k.getKundhandelseId()).containsExactly("TESTKOP-1", "TESTKOP-2");
		});
		assertThat(response.getDelfragor()).hasSize(1).first().satisfies(subQuery -> {
			assertThat(subQuery.getProducent()).isEqualTo("Testköpings kommun");
			assertThat(subQuery.getStatus()).isEqualTo("OK");
			assertThat(subQuery.getHttpCode()).isEqualTo(200);
			assertThat(subQuery.getMessage()).isNull();
		});
		assertThat(response.getMetadata().getOnskatSprak()).isEqualTo("sv");
		assertThat(response.getMetadata().getParter()).isEqualTo(request.getFraga().getParter());
	}

	@Test
	@SuppressWarnings("unchecked")
	void answerWithPaginationWithoutLimit() {
		final var request = fraga();
		request.getFraga()
			.withParter(List.of(Part.create().withKund(kund()), Part.create().withKund(kund())))
			.withBehandling(Behandling.create()
				.withSortering(List.of(Sortering.create().withAttribut("TIDPUNKT").withStigande(true)))
				.withPaginering(Paginering.create().withOffset(1)));
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		when(repository.findAll(any(Specification.class), eq(new OffsetPageable(1, Integer.MAX_VALUE))))
			.thenReturn(new PageImpl<>(List.of(entity("TESTKOP-2")), new OffsetPageable(1, Integer.MAX_VALUE), 2));

		final var response = service.answer(MUNICIPALITY_ID, request, null);

		assertThat(response.getKundhandelser()).hasSize(2).allSatisfy(perParty -> {
			assertThat(perParty.getTotaltAntalKundhandelser()).isEqualTo(2);
			assertThat(perParty.getKundhandelserForPart()).hasSize(1);
		});
		assertThat(response.getDelfragor()).hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void answerWithLimit() {
		final var request = fraga();
		request.getFraga().withBehandling(Behandling.create()
			.withSortering(List.of(Sortering.create().withAttribut("RUBRIK").withStigande(false)))
			.withPaginering(Paginering.create().withOffset(0).withLimit(5)));
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		when(repository.findAll(any(Specification.class), eq(new OffsetPageable(0, 5))))
			.thenReturn(new PageImpl<ActivityEntity>(List.of(), new OffsetPageable(0, 5), 0));

		assertThat(service.answer(MUNICIPALITY_ID, request, null).getKundhandelser().getFirst().getTotaltAntalKundhandelser()).isZero();
	}

	@Test
	void emptyPartyIsRejected() {
		final var request = fraga();
		request.getFraga().withParter(List.of(Part.create()));
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);

		assertBadRequest(request, "Varje part måste innehålla kund, ärende eller båda");
	}

	@Test
	void paginationWithoutSortingIsRejected() {
		final var request = fraga();
		request.getFraga().withBehandling(Behandling.create().withPaginering(Paginering.create().withOffset(0)));
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);

		assertBadRequest(request, "Paginering kräver att sortering anges");

		request.getFraga().getBehandling().withSortering(List.of());
		assertBadRequest(request, "Paginering kräver att sortering anges");
	}

	@Test
	void startDateAfterEndDateIsRejected() {
		final var request = fraga();
		request.getFraga().withStartDatum("2026-09-02").withSlutDatum("2026-09-01");
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);

		assertBadRequest(request, "startDatum får inte vara efter slutDatum");
	}

	@Test
	void unknownMunicipality() {
		when(producerService.getProducer("1480")).thenThrow(Problem.valueOf(NOT_FOUND, "saknas"));

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.answer("1480", fraga(), null));
		verify(producerService).getProducer("1480");
		verifyNoInteractions(repository);
	}

	private void assertBadRequest(final KundhandelseFragaRequest request, final String detail) {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.answer(MUNICIPALITY_ID, request, null))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo(detail);
			});
	}
}
