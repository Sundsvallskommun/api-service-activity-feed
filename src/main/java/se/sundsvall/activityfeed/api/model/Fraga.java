package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import se.sundsvall.activityfeed.api.validation.ValidFraga;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * The query itself: which parties it covers and how the response is filtered, sorted and paginated.
 */
@Schema(description = "Frågan")
@ValidFraga
public class Fraga {

	@Schema(description = "Parter som frågan gäller (kund, ärende eller båda)", requiredMode = REQUIRED)
	@NotEmpty(message = "fraga.parter måste innehålla minst en part")
	// Each party becomes its own database query, so the number is capped. The standard sets no maximum.
	@Size(max = 100, message = "fraga.parter får innehålla högst {max} parter")
	private List<@Valid Part> parter;

	@Schema(description = "Kundhändelsetyper, t.ex. REFKOM.BYGGLOV eller REFKOM.BYGGLOV.ANSOKAN_MOTTAGEN. Ett prefix matchar alla typer under det.")
	@Size(max = 100, message = "kundhandelseTyper får innehålla högst {max} typer")
	// The pattern in the JSON schema of Skatteverket's API definition, plus a lone producer prefix (e.g. REFKOM) as in the
	// schema's example. The producer must not reject queries that the forwarding service lets through. The length matches
	// the column activity_type.
	private List<@Pattern(regexp = "^[A-Z]+(?:\\.[A-Z0-9_.]+)?$", message = "kundhandelseTyper ska ha formatet PRODUCENT, PRODUCENT.ARENDETYP eller PRODUCENT.ARENDETYP.HANDELSETYP (versaler, siffror, understreck)") @Size(
		max = 200,
		message = "kundhandelseTyper: för lång typ") String> kundhandelseTyper;

	@Schema(description = "Minst en av taggarna ska finnas på kundhändelsen")
	@Size(max = 100, message = "taggar får innehålla högst {max} taggar")
	private List<@Size(max = 100, message = "taggar: för lång tagg") String> taggar;

	@Schema(description = "Från och med datum (svensk tid)", examples = "2026-08-01")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "startDatum ska ha formatet ÅÅÅÅ-MM-DD")
	private String startDatum;

	@Schema(description = "Till och med datum (svensk tid)", examples = "2026-09-30")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "slutDatum ska ha formatet ÅÅÅÅ-MM-DD")
	private String slutDatum;

	@Valid
	private Behandling behandling;

	public static Fraga create() {
		return new Fraga();
	}

	public List<Part> getParter() {
		return parter;
	}

	public void setParter(final List<Part> parter) {
		this.parter = parter;
	}

	public Fraga withParter(final List<Part> parter) {
		this.parter = parter;
		return this;
	}

	public List<String> getKundhandelseTyper() {
		return kundhandelseTyper;
	}

	public void setKundhandelseTyper(final List<String> kundhandelseTyper) {
		this.kundhandelseTyper = kundhandelseTyper;
	}

	public Fraga withKundhandelseTyper(final List<String> kundhandelseTyper) {
		this.kundhandelseTyper = kundhandelseTyper;
		return this;
	}

	public List<String> getTaggar() {
		return taggar;
	}

	public void setTaggar(final List<String> taggar) {
		this.taggar = taggar;
	}

	public Fraga withTaggar(final List<String> taggar) {
		this.taggar = taggar;
		return this;
	}

	public String getStartDatum() {
		return startDatum;
	}

	public void setStartDatum(final String startDatum) {
		this.startDatum = startDatum;
	}

	public Fraga withStartDatum(final String startDatum) {
		this.startDatum = startDatum;
		return this;
	}

	public String getSlutDatum() {
		return slutDatum;
	}

	public void setSlutDatum(final String slutDatum) {
		this.slutDatum = slutDatum;
	}

	public Fraga withSlutDatum(final String slutDatum) {
		this.slutDatum = slutDatum;
		return this;
	}

	public Behandling getBehandling() {
		return behandling;
	}

	public void setBehandling(final Behandling behandling) {
		this.behandling = behandling;
	}

	public Fraga withBehandling(final Behandling behandling) {
		this.behandling = behandling;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parter, kundhandelseTyper, taggar, startDatum, slutDatum, behandling);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Fraga other)) {
			return false;
		}
		return Objects.equals(parter, other.parter) && Objects.equals(kundhandelseTyper, other.kundhandelseTyper) && Objects.equals(taggar, other.taggar) && Objects.equals(startDatum, other.startDatum) && Objects.equals(slutDatum, other.slutDatum)
			&& Objects.equals(behandling, other.behandling);
	}

	@Override
	public String toString() {
		return "Fraga [parter=" + parter +
			", kundhandelseTyper=" + kundhandelseTyper +
			", taggar=" + taggar +
			", startDatum=" + startDatum +
			", slutDatum=" + slutDatum +
			", behandling=" + behandling +
			"]";
	}
}
