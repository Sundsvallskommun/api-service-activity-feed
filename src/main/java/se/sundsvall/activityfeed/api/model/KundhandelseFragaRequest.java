package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import se.sundsvall.dept44.util.PiiMasker;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Query from the forwarding service, same schema as in Skatteverket's API definition for Mina ärenden kundhändelser.
 */
@Schema(name = "KundhandelseFragaRequest", description = "Fråga om kundhändelser")
public class KundhandelseFragaRequest {

	@Schema(requiredMode = REQUIRED)
	@NotNull
	@Valid
	private Fraga fraga;

	@Schema(description = "Personnummer för användaren som begär uppgifterna", examples = "194903012658", requiredMode = REQUIRED)
	@NotBlank
	@Pattern(regexp = "^\\d{12}$", message = "anvandare ska vara 12 siffror")
	private String anvandare;

	public static KundhandelseFragaRequest create() {
		return new KundhandelseFragaRequest();
	}

	public Fraga getFraga() {
		return fraga;
	}

	public void setFraga(final Fraga fraga) {
		this.fraga = fraga;
	}

	public KundhandelseFragaRequest withFraga(final Fraga fraga) {
		this.fraga = fraga;
		return this;
	}

	public String getAnvandare() {
		return anvandare;
	}

	public void setAnvandare(final String anvandare) {
		this.anvandare = anvandare;
	}

	public KundhandelseFragaRequest withAnvandare(final String anvandare) {
		this.anvandare = anvandare;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fraga, anvandare);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final KundhandelseFragaRequest other)) {
			return false;
		}
		return Objects.equals(fraga, other.fraga) && Objects.equals(anvandare, other.anvandare);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("KundhandelseFragaRequest [fraga=").append(fraga)
			.append(", anvandare=").append(PiiMasker.maskPersonalNumber(anvandare))
			.append("]").toString();
	}
}
