package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * The errand a party refers to.
 */
@Schema(description = "Används för att smalna av frågan till ett specifikt ärende")
public class Arende {

	@Schema(description = "Ett värde som unikt identifierar ärendet", examples = "BYGG-2026-00123", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 100, message = "arende.identifierare får vara högst 100 tecken")
	private String identifierare;

	@Schema(description = "Typ av identifierare", examples = "Diarienummer", allowableValues = {
		"Konsumentreferens", "Producentarendereferens", "Diarienummer", "Kvittensnummer"
	}, requiredMode = REQUIRED)
	@NotBlank
	@Pattern(regexp = "^(Konsumentreferens|Producentarendereferens|Diarienummer|Kvittensnummer)$", message = "arende.typ har ogiltigt värde")
	private String typ;

	public static Arende create() {
		return new Arende();
	}

	public String getIdentifierare() {
		return identifierare;
	}

	public void setIdentifierare(final String identifierare) {
		this.identifierare = identifierare;
	}

	public Arende withIdentifierare(final String identifierare) {
		this.identifierare = identifierare;
		return this;
	}

	public String getTyp() {
		return typ;
	}

	public void setTyp(final String typ) {
		this.typ = typ;
	}

	public Arende withTyp(final String typ) {
		this.typ = typ;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifierare, typ);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Arende other)) {
			return false;
		}
		return Objects.equals(identifierare, other.identifierare) && Objects.equals(typ, other.typ);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("Arende [identifierare=").append(identifierare)
			.append(", typ=").append(typ)
			.append("]").toString();
	}
}
