package se.sundsvall.activityfeed.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.Objects;

/**
 * A party is identified as a customer, an errand or both. If both are given, the response must match both.
 */
@Schema(description = "En part identifieras som kund, ärende eller båda")
public class Part {

	@Valid
	private Kund kund;

	@Valid
	private Arende arende;

	public static Part create() {
		return new Part();
	}

	public Kund getKund() {
		return kund;
	}

	public void setKund(final Kund kund) {
		this.kund = kund;
	}

	public Part withKund(final Kund kund) {
		this.kund = kund;
		return this;
	}

	public Arende getArende() {
		return arende;
	}

	public void setArende(final Arende arende) {
		this.arende = arende;
	}

	public Part withArende(final Arende arende) {
		this.arende = arende;
		return this;
	}

	/**
	 * @return true if neither customer nor errand is given
	 */
	@JsonIgnore
	@Schema(hidden = true)
	public boolean isEmpty() {
		return kund == null && arende == null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(kund, arende);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Part other)) {
			return false;
		}
		return Objects.equals(kund, other.kund) && Objects.equals(arende, other.arende);
	}

	@Override
	public String toString() {
		return "Part [kund=" + kund +
			", arende=" + arende +
			"]";
	}
}
