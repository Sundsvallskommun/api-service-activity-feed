package se.sundsvall.minaarenden.service;

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
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.minaarenden.api.model.Part;
import se.sundsvall.minaarenden.integration.db.KundhandelseRepository;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity.UNIK_KUNDHANDELSE_ID;
import static se.sundsvall.minaarenden.service.TestdataFactory.MUNICIPALITY_ID;
import static se.sundsvall.minaarenden.service.TestdataFactory.PRODUCENT;
import static se.sundsvall.minaarenden.service.TestdataFactory.entity;
import static se.sundsvall.minaarenden.service.TestdataFactory.nyKundhandelse;

@ExtendWith(MockitoExtension.class)
class KundhandelseServiceTest {

	@Mock
	private KundhandelseRepository repository;

	@Mock
	private ProducentService producentService;

	@InjectMocks
	private KundhandelseService service;

	@Captor
	private ArgumentCaptor<KundhandelseEntity> entityCaptor;

	@Test
	void sparaNyOchErsattBefintlig() {
		final var befintlig = entity("TESTKOP-2").withRubrik("Gammal rubrik");
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		when(producentService.standardVersion()).thenReturn("6.1");
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.empty());
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-2")).thenReturn(Optional.of(befintlig));

		final var antal = service.spara(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"), nyKundhandelse("TESTKOP-2")));

		assertThat(antal).isEqualTo(2);
		verify(repository, org.mockito.Mockito.times(2)).save(entityCaptor.capture());
		verify(repository).flush();
		assertThat(entityCaptor.getAllValues().getFirst().getId()).isNull();
		assertThat(entityCaptor.getAllValues().getLast()).isSameAs(befintlig);
		assertThat(befintlig.getRubrik()).isEqualTo("Beslut om bygglov");
	}

	@Test
	void felPrefixAvvisasInnanNagotSparas() {
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		final var ny = nyKundhandelse("X-1").withKundhandelseTyp("ANNAN.BYGGLOV.BESLUT");

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.spara(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"), ny)))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo("kundhandelseTyp måste börja med producentprefixet TESTKOP. (X-1)");
			});
		verify(repository, never()).save(any());
	}

	@Test
	void tomPartAvvisas() {
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.spara(MUNICIPALITY_ID, List.of(nyKundhandelse("X-1").withPart(Part.create()))))
			.satisfies(problem -> assertThat(problem.getDetail()).isEqualTo("part måste innehålla kund, ärende eller båda (X-1)"));
	}

	@Test
	void samtidigInlasningGer409() {
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		when(producentService.standardVersion()).thenReturn("6.1");
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.empty());
		doThrow(databasfel(UNIK_KUNDHANDELSE_ID)).when(repository).flush();

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.spara(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"))))
			.satisfies(problem -> assertThat(problem.getStatus()).isEqualTo(CONFLICT));
	}

	@Test
	void annatDatabasfelLamnasVidare() {
		final var fel = databasfel("PRIMARY");
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		when(producentService.standardVersion()).thenReturn("6.1");
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.empty());
		doThrow(fel).when(repository).flush();

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> service.spara(MUNICIPALITY_ID, List.of(nyKundhandelse("TESTKOP-1"))))
			.isSameAs(fel);
	}

	@Test
	void arKundhandelseIdKrock() {
		assertThat(KundhandelseService.arKundhandelseIdKrock(databasfel(UNIK_KUNDHANDELSE_ID))).isTrue();
		assertThat(KundhandelseService.arKundhandelseIdKrock(databasfel("PRIMARY"))).isFalse();
		assertThat(KundhandelseService.arKundhandelseIdKrock(databasfel(null))).isFalse();
		assertThat(KundhandelseService.arKundhandelseIdKrock(new DataIntegrityViolationException("Data too long"))).isFalse();
	}

	private static DataIntegrityViolationException databasfel(final String constraintName) {
		return new DataIntegrityViolationException("Duplicate entry", new ConstraintViolationException("Duplicate entry", new SQLException(), constraintName));
	}

	@Test
	void hamta() {
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.of(entity("TESTKOP-1")));

		final var cachad = service.hamta(MUNICIPALITY_ID, "TESTKOP-1");

		assertThat(cachad.getKundhandelse().getKundhandelseId()).isEqualTo("TESTKOP-1");
		assertThat(cachad.getTaggar()).containsExactly("bygglov");
		verify(producentService).hamta(MUNICIPALITY_ID);
	}

	@Test
	void hamtaSaknasGer404() {
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "X")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.hamta(MUNICIPALITY_ID, "X"))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).isEqualTo("Kundhändelsen X finns inte");
			});
	}

	@Test
	void taBort() {
		final var entity = entity("TESTKOP-1");
		when(repository.findByMunicipalityIdAndKundhandelseId(MUNICIPALITY_ID, "TESTKOP-1")).thenReturn(Optional.of(entity));

		service.taBort(MUNICIPALITY_ID, "TESTKOP-1");

		verify(repository).delete(entity);
	}

	@Test
	void antal() {
		when(repository.countByMunicipalityId(MUNICIPALITY_ID)).thenReturn(7L);

		assertThat(service.antal(MUNICIPALITY_ID)).isEqualTo(7);
		verify(producentService).hamta(MUNICIPALITY_ID);
	}
}
