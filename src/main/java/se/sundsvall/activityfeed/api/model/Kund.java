package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.util.PiiMasker;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * The customer a party refers to. identifierare is a personal identity number, coordination number or organisation
 * number, so toString() masks it.
 */
@Schema(description = "Vem frågan gäller")
public class Kund {

	@Schema(description = "Ett värde som unikt identifierar kunden, 12 siffror", examples = "199009090000", requiredMode = REQUIRED)
	@NotBlank
	@Pattern(regexp = "^\\d{12}$", message = "kund.identifierare ska vara 12 siffror")
	private String identifierare;

	@Schema(description = "Löpnummer som skiljer flera enskilda firmor åt", nullable = true)
	@Size(max = 20, message = "kund.tillagg får vara högst 20 tecken")
	private String tillagg;

	@Schema(description = "Typ av identifierare", examples = "Personnummer", allowableValues = {
		"Personnummer", "Organisationsnummer", "Samordningsnummer"
	}, requiredMode = REQUIRED)
	@NotBlank
	@Pattern(regexp = "^(Personnummer|Organisationsnummer|Samordningsnummer)$", message = "kund.typ har ogiltigt värde")
	private String typ;

	public static Kund create() {
		return new Kund();
	}

	public String getIdentifierare() {
		return identifierare;
	}

	public void setIdentifierare(final String identifierare) {
		this.identifierare = identifierare;
	}

	public Kund withIdentifierare(final String identifierare) {
		this.identifierare = identifierare;
		return this;
	}

	public String getTillagg() {
		return tillagg;
	}

	public void setTillagg(final String tillagg) {
		this.tillagg = tillagg;
	}

	public Kund withTillagg(final String tillagg) {
		this.tillagg = tillagg;
		return this;
	}

	public String getTyp() {
		return typ;
	}

	public void setTyp(final String typ) {
		this.typ = typ;
	}

	public Kund withTyp(final String typ) {
		this.typ = typ;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifierare, tillagg, typ);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Kund other)) {
			return false;
		}
		return Objects.equals(identifierare, other.identifierare) && Objects.equals(tillagg, other.tillagg) && Objects.equals(typ, other.typ);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("Kund [identifierare=").append(PiiMasker.maskPersonalNumber(identifierare))
			.append(", tillagg=").append(tillagg)
			.append(", typ=").append(typ)
			.append("]").toString();
	}
}
