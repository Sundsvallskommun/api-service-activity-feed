package se.sundsvall.minaarenden.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.minaarenden.api.model.CachadKundhandelse;
import se.sundsvall.minaarenden.api.model.NyKundhandelse;
import se.sundsvall.minaarenden.integration.db.KundhandelseRepository;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;
import se.sundsvall.minaarenden.service.mapper.KundhandelseMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity.UNIK_KUNDHANDELSE_ID;

/**
 * Underhåll av ärendecachen: verksamhetssystemet skickar in kundhändelser, cachen svarar på frågor.
 */
@Service
public class KundhandelseService {

	private static final Logger LOG = LoggerFactory.getLogger(KundhandelseService.class);

	private final KundhandelseRepository repository;
	private final ProducentService producentService;

	public KundhandelseService(final KundhandelseRepository repository, final ProducentService producentService) {
		this.repository = repository;
		this.producentService = producentService;
	}

	/**
	 * Skapar eller ersätter (samma kundhandelseId inom kommunen) kundhändelser.
	 *
	 * <p>
	 * Hela listan sparas i en transaktion. Om två anrop samtidigt skapar samma nya kundhandelseId skyddar det unika
	 * databasvillkoret mot dubbletter; det anrop som förlorar får 409 och kan skickas om oförändrat, eftersom operationen
	 * är idempotent. Andra databasfel lämnas vidare, eftersom de inte går att lösa genom att skicka om anropet.
	 *
	 * @param  municipalityId kommun
	 * @param  nya            kundhändelser att spara
	 * @return                antal sparade kundhändelser
	 */
	@Transactional
	public int spara(final String municipalityId, final List<NyKundhandelse> nya) {
		final var producent = producentService.hamta(municipalityId);
		final var prefix = producent.prefix() + ".";
		for (final var ny : nya) {
			if (ny.getPart().isEmpty()) {
				throw Problem.valueOf(BAD_REQUEST, "part måste innehålla kund, ärende eller båda (" + ny.getKundhandelseId() + ")");
			}
			if (!ny.getKundhandelseTyp().startsWith(prefix)) {
				throw Problem.valueOf(BAD_REQUEST, "kundhandelseTyp måste börja med producentprefixet " + prefix + " (" + ny.getKundhandelseId() + ")");
			}
		}
		try {
			for (final var ny : nya) {
				final var entity = repository.findByMunicipalityIdAndKundhandelseId(municipalityId, ny.getKundhandelseId()).orElseGet(KundhandelseEntity::create);
				repository.save(KundhandelseMapper.toEntity(municipalityId, producent, producentService.standardVersion(), ny, entity));
			}
			repository.flush();
		} catch (final DataIntegrityViolationException e) {
			if (!arKundhandelseIdKrock(e)) {
				throw e;
			}
			LOG.warn("Samtidig inläsning av samma kundhandelseId: {}", e.getMostSpecificCause().getMessage());
			throw Problem.valueOf(CONFLICT, "Samtidig ändring av samma kundhändelse, skicka anropet igen");
		}
		return nya.size();
	}

	/**
	 * @param  e databasfelet
	 * @return   true om felet är en krock på det unika villkoret för kundhandelseId inom kommunen
	 */
	static boolean arKundhandelseIdKrock(final DataIntegrityViolationException e) {
		return Stream.<Throwable>iterate(e, Objects::nonNull, Throwable::getCause)
			.filter(ConstraintViolationException.class::isInstance)
			.map(ConstraintViolationException.class::cast)
			.map(ConstraintViolationException::getConstraintName)
			.anyMatch(UNIK_KUNDHANDELSE_ID::equalsIgnoreCase);
	}

	@Transactional(readOnly = true)
	public CachadKundhandelse hamta(final String municipalityId, final String kundhandelseId) {
		return KundhandelseMapper.toCachadKundhandelse(finn(municipalityId, kundhandelseId));
	}

	@Transactional
	public void taBort(final String municipalityId, final String kundhandelseId) {
		repository.delete(finn(municipalityId, kundhandelseId));
	}

	@Transactional(readOnly = true)
	public long antal(final String municipalityId) {
		producentService.hamta(municipalityId);
		return repository.countByMunicipalityId(municipalityId);
	}

	private KundhandelseEntity finn(final String municipalityId, final String kundhandelseId) {
		producentService.hamta(municipalityId);
		return repository.findByMunicipalityIdAndKundhandelseId(municipalityId, kundhandelseId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Kundhändelsen " + kundhandelseId + " finns inte"));
	}
}
