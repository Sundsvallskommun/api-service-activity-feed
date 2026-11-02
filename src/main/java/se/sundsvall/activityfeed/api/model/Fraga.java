package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import se.sundsvall.activityfeed.api.validation.ActivityTypeFormat;
import se.sundsvall.activityfeed.api.validation.Limits;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * The query itself: which parties it covers and how the response is filtered, sorted and paginated.
 */
@Schema(description = "Frågan")
public class Fraga {

	@Schema(description = "Parter som frågan gäller (kund, ärende eller båda)", requiredMode = REQUIRED)
	@NotEmpty(message = "fraga.parter måste innehålla minst en part")
	@Size(max = Limits.MAX_PARTIES, message = "fraga.parter får innehålla högst " + Limits.MAX_PARTIES + " parter")
	private List<@Valid Part> parter;

	@Schema(description = "Kundhändelsetyper, t.ex. REFKOM.BYGGLOV eller REFKOM.BYGGLOV.ANSOKAN_MOTTAGEN. Ett prefix matchar alla typer under det.")
	@Size(max = Limits.MAX_ACTIVITY_TYPES, message = "kundhandelseTyper får innehålla högst " + Limits.MAX_ACTIVITY_TYPES + " typer")
	private List<@Pattern(regexp = ActivityTypeFormat.QUERY_PATTERN, message = "kundhandelseTyper har ogiltigt format") @Size(max = ActivityTypeFormat.MAX_LENGTH, message = "kundhandelseTyper: för lång typ") String> kundhandelseTyper;

	@Schema(description = "Minst en av taggarna ska finnas på kundhändelsen")
	@Size(max = Limits.MAX_TAGS, message = "taggar får innehålla högst " + Limits.MAX_TAGS + " taggar")
	private List<@Size(max = Limits.MAX_TAG_LENGTH, message = "taggar: för lång tagg") String> taggar;

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
		return new StringBuilder()
			.append("Fraga [parter=").append(parter)
			.append(", kundhandelseTyper=").append(kundhandelseTyper)
			.append(", taggar=").append(taggar)
			.append(", startDatum=").append(startDatum)
			.append(", slutDatum=").append(slutDatum)
			.append(", behandling=").append(behandling)
			.append("]").toString();
	}
}
