package se.sundsvall.minaarenden.integration.db.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.minaarenden.api.model.Arende;
import se.sundsvall.minaarenden.api.model.Kund;
import se.sundsvall.minaarenden.api.model.Sortering;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity_;

import static org.springframework.data.jpa.domain.Specification.unrestricted;

/**
 * Databasfilter och sortering som motsvarar fälten i fraga.
 */
public final class KundhandelseSpecification {

	private KundhandelseSpecification() {}

	public static Specification<KundhandelseEntity> withMunicipalityId(final String municipalityId) {
		return (root, query, cb) -> cb.equal(root.get(KundhandelseEntity_.municipalityId), municipalityId);
	}

	/**
	 * Parten matchar om både angiven kund och angivet ärende stämmer (spec: "svaret uppfyller båda kriterierna").
	 */
	public static Specification<KundhandelseEntity> forPart(final Kund kund, final Arende arende) {
		return (root, query, cb) -> {
			final List<Predicate> predikat = new ArrayList<>();
			if (kund != null) {
				predikat.add(cb.equal(root.get(KundhandelseEntity_.kundIdentifierare), kund.getIdentifierare()));
				predikat.add(cb.equal(root.get(KundhandelseEntity_.kundTyp), kund.getTyp()));
				if (kund.getTillagg() != null && !kund.getTillagg().isBlank()) {
					predikat.add(cb.equal(root.get(KundhandelseEntity_.kundTillagg), kund.getTillagg()));
				}
			}
			if (arende != null) {
				predikat.add(cb.equal(root.get(KundhandelseEntity_.arendeIdentifierare), arende.getIdentifierare()));
				predikat.add(cb.equal(root.get(KundhandelseEntity_.arendeTyp), arende.getTyp()));
			}
			return cb.and(predikat.toArray(Predicate[]::new));
		};
	}

	/**
	 * En kundhändelsetyp matchar filtret om den är lika med det eller ligger under det (prefix följt av punkt).
	 */
	public static Specification<KundhandelseEntity> withKundhandelseTyper(final List<String> typer) {
		if (typer == null || typer.isEmpty()) {
			return unrestricted();
		}
		return (root, query, cb) -> cb.or(typer.stream()
			.map(typ -> cb.or(
				cb.equal(root.get(KundhandelseEntity_.kundhandelseTyp), typ),
				cb.like(root.get(KundhandelseEntity_.kundhandelseTyp), escapeLike(typ) + ".%", '!')))
			.toArray(Predicate[]::new));
	}

	/**
	 * Minst en av taggarna ska finnas på händelsen. Uttrycks som EXISTS i stället för JOIN + DISTINCT så att sortering på
	 * uttryck (t.ex. LOWER(rubrik)) fungerar och inga dubbletter uppstår.
	 */
	public static Specification<KundhandelseEntity> withNagonAvTaggarna(final List<String> taggar) {
		if (taggar == null || taggar.isEmpty()) {
			return unrestricted();
		}
		return (root, query, cb) -> {
			final var subquery = query.subquery(String.class);
			final var inner = subquery.from(KundhandelseEntity.class);
			final var tagg = inner.join(KundhandelseEntity_.taggar);
			subquery.select(inner.get(KundhandelseEntity_.id)).where(cb.equal(inner.get(KundhandelseEntity_.id), root.get(KundhandelseEntity_.id)), tagg.in(taggar));
			return cb.exists(subquery);
		};
	}

	public static Specification<KundhandelseEntity> withTidpunktFrom(final Instant from) {
		return from == null ? unrestricted() : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(KundhandelseEntity_.tidpunkt), from);
	}

	public static Specification<KundhandelseEntity> withTidpunktTom(final Instant tom) {
		return tom == null ? unrestricted() : (root, query, cb) -> cb.lessThanOrEqualTo(root.get(KundhandelseEntity_.tidpunkt), tom);
	}

	/**
	 * Sortering enligt fraga.behandling.sortering, utförd i databasen. Utan angiven sortering nyast först. Tidpunkt
	 * sorteras
	 * på den lagrade DATETIME-kolumnen, inte på RFC 3339-texten, så ordningen blir kronologisk även när UTC-offset skiljer
	 * sig (t.ex. vid omställning till vintertid). Primärnyckeln läggs sist som avgörande kriterium så att paginering blir
	 * stabil vid lika värden. Sorteringen hoppas över i COUNT-frågan.
	 */
	public static Specification<KundhandelseEntity> sorteradEfter(final List<Sortering> sortering) {
		return (root, query, cb) -> {
			if (query == null || Long.class.equals(query.getResultType())) {
				return null;
			}
			final List<Order> ordning = new ArrayList<>();
			if (sortering == null || sortering.isEmpty()) {
				ordning.add(cb.desc(root.get(KundhandelseEntity_.tidpunkt)));
				ordning.add(cb.desc(root.get(KundhandelseEntity_.id)));
			} else {
				for (final var s : sortering) {
					final var uttryck = sorteringsuttryck(root, cb, s.getAttribut());
					ordning.add(Boolean.TRUE.equals(s.getStigande()) ? cb.asc(uttryck) : cb.desc(uttryck));
				}
				ordning.add(cb.asc(root.get(KundhandelseEntity_.id)));
			}
			query.orderBy(ordning);
			return null;
		};
	}

	static Expression<?> sorteringsuttryck(final Root<KundhandelseEntity> root, final CriteriaBuilder cb, final String attribut) {
		return switch (attribut) {
			case "RUBRIK" -> cb.lower(root.get(KundhandelseEntity_.rubrik));
			case "BESKRIVNING" -> cb.lower(root.get(KundhandelseEntity_.beskrivning));
			case "PRODUCENT" -> cb.lower(root.get(KundhandelseEntity_.producent));
			case "TIDPUNKT" -> root.get(KundhandelseEntity_.tidpunkt);
			case "KUNDHANDELSETYP" -> root.get(KundhandelseEntity_.kundhandelseTyp);
			case "PRODUCENTARENDETKRAVERKUNDATGARD" -> root.get(KundhandelseEntity_.producentarendetKraverKundatgard);
			case "PRODUCENTARENDETKLART" -> root.get(KundhandelseEntity_.producentarendetKlart);
			default -> throw new IllegalArgumentException("Okänt sorteringsattribut: " + attribut);
		};
	}

	static String escapeLike(final String varde) {
		return varde.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}
}
