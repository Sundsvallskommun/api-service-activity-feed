package se.sundsvall.activityfeed.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import se.sundsvall.activityfeed.api.model.Part;
import se.sundsvall.activityfeed.integration.db.ActivityRepository;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.activityfeed.integration.db.model.ActivityEntity.UNIQUE_ACTIVITY_ID;
import static se.sundsvall.activityfeed.service.TestdataFactory.MUNICIPALITY_ID;
import static se.sundsvall.activityfeed.service.TestdataFactory.PRODUCER;
import static se.sundsvall.activityfeed.service.TestdataFactory.entity;
import static se.sundsvall.activityfeed.service.TestdataFactory.nyKundhandelse;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

	@Mock
	private ActivityRepository repository;

	@Mock
	private ProducerService producerService;

	@InjectMocks
	private ActivityService service;

	@Captor
	private ArgumentCaptor<ActivityEntity> entityCaptor;

	@Test
	void saveNewAndReplaceExisting() {
		final var existing = entity("TESTKOP-2").withTitle("Gammal rubrik");
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		when(producerService.standardVersion()).thenReturn("6.1");
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.empty());
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-2")).thenReturn(Optional.of(existing));

		final var count = service.save(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"), nyKundhandelse("TESTKOP-2")));

		assertThat(count).isEqualTo(2);
		verify(repository, times(2)).save(entityCaptor.capture());
		verify(repository).flush();
		assertThat(entityCaptor.getAllValues().getFirst().getId()).isNull();
		assertThat(entityCaptor.getAllValues().getLast()).isSameAs(existing);
		assertThat(existing.getTitle()).isEqualTo("Beslut om bygglov");
	}

	@Test
	void wrongPrefixIsRejectedBeforeAnythingIsSaved() {
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		final var invalid = nyKundhandelse("X-1").withKundhandelseTyp("ANNAN.BYGGLOV.BESLUT");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.save(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"), invalid)))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("kundhandelseTyp måste börja med producentprefixet TESTKOP. (X-1)");
			});
		verify(repository, never()).save(any());
	}

	@Test
	void emptyPartyIsRejected() {
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.save(MUNICIPALITY_ID, List.of(nyKundhandelse("X-1").withPart(Part.create()))))
			.satisfies(problem -> assertThat(problem.getDetail()).isEqualTo("part måste innehålla kund, ärende eller båda (X-1)"));
	}

	@Test
	void concurrentIngestGives409() {
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		when(producerService.standardVersion()).thenReturn("6.1");
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.empty());
		doThrow(databaseError(UNIQUE_ACTIVITY_ID)).when(repository).flush();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.save(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"))))
			.satisfies(problem -> assertThat(problem.getStatus()).isEqualTo(CONFLICT));
	}

	@Test
	void otherDatabaseErrorIsPropagated() {
		final var error = databaseError("PRIMARY");
		when(producerService.getProducer(MUNICIPALITY_ID)).thenReturn(PRODUCER);
		when(producerService.standardVersion()).thenReturn("6.1");
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.empty());
		doThrow(error).when(repository).flush();

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> service.save(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"))))
			.isSameAs(error);
	}

	@Test
	void isActivityIdCollision() {
		assertThat(ActivityService.isActivityIdCollision(databaseError(UNIQUE_ACTIVITY_ID))).isTrue();
		assertThat(ActivityService.isActivityIdCollision(databaseError("PRIMARY"))).isFalse();
		assertThat(ActivityService.isActivityIdCollision(databaseError(null))).isFalse();
		assertThat(ActivityService.isActivityIdCollision(new DataIntegrityViolationException("Data too long"))).isFalse();
	}

	private static DataIntegrityViolationException databaseError(final String constraintName) {
		return new DataIntegrityViolationException("Duplicate entry", new ConstraintViolationException("Duplicate entry", new SQLException(), constraintName));
	}

	@Test
	void read() {
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.of(entity("TESTKOP-1")));

		final var cached = service.read(MUNICIPALITY_ID, "TESTKOP-1");

		assertThat(cached.getKundhandelse().getKundhandelseId()).isEqualTo("TESTKOP-1");
		assertThat(cached.getTaggar()).containsExactly("bygglov");
		verify(producerService).getProducer(MUNICIPALITY_ID);
	}

	@Test
	void readMissingGives404() {
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "X")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.read(MUNICIPALITY_ID, "X"))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).isEqualTo("Kundhändelsen X finns inte");
			});
	}

	@Test
	void delete() {
		final var entity = entity("TESTKOP-1");
		when(repository.findByMunicipalityIdAndActivityId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, "TESTKOP-1");

		verify(repository).delete(entity);
	}

	@Test
	void count() {
		when(repository.countByMunicipalityId(MUNICIPALITY_ID)).thenReturn(7L);

		assertThat(service.count(MUNICIPALITY_ID)).isEqualTo(7);
		verify(producerService).getProducer(MUNICIPALITY_ID);
	}
}
