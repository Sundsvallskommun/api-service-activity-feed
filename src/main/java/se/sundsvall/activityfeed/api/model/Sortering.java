package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Sortering på ett attribut")
public class Sortering {

	@Schema(examples = "TIDPUNKT", allowableValues = {
		"RUBRIK", "BESKRIVNING", "PRODUCENT", "TIDPUNKT", "KUNDHANDELSETYP", "PRODUCENTARENDETKRAVERKUNDATGARD", "PRODUCENTARENDETKLART"
	}, requiredMode = REQUIRED)
	@NotNull
	@Pattern(regexp = "^(RUBRIK|BESKRIVNING|PRODUCENT|TIDPUNKT|KUNDHANDELSETYP|PRODUCENTARENDETKRAVERKUNDATGARD|PRODUCENTARENDETKLART)$", message = "sortering.attribut har ogiltigt värde")
	private String attribut;

	@Schema(requiredMode = REQUIRED)
	@NotNull
	private Boolean stigande;

	public static Sortering create() {
		return new Sortering();
	}

	public String getAttribut() {
		return attribut;
	}

	public void setAttribut(final String attribut) {
		this.attribut = attribut;
	}

	public Sortering withAttribut(final String attribut) {
		this.attribut = attribut;
		return this;
	}

	public Boolean getStigande() {
		return stigande;
	}

	public void setStigande(final Boolean stigande) {
		this.stigande = stigande;
	}

	public Sortering withStigande(final Boolean stigande) {
		this.stigande = stigande;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attribut, stigande);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Sortering other)) {
			return false;
		}
		return Objects.equals(attribut, other.attribut) && Objects.equals(stigande, other.stigande);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("Sortering [attribut=").append(attribut)
			.append(", stigande=").append(stigande)
			.append("]").toString();
	}
}
