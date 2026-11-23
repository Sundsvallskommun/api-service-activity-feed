package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Echoes the query in the response.
 */
@Schema(description = "Frågan som svaret gäller")
public class Metadata {

	private List<Part> parter;

	private List<String> kundhandelseTyper;

	private List<String> taggar;

	private String startDatum;

	private String slutDatum;

	private String onskatSprak;

	private Behandling behandling;

	public static Metadata create() {
		return new Metadata();
	}

	public List<Part> getParter() {
		return parter;
	}

	public void setParter(final List<Part> parter) {
		this.parter = parter;
	}

	public Metadata withParter(final List<Part> parter) {
		this.parter = parter;
		return this;
	}

	public List<String> getKundhandelseTyper() {
		return kundhandelseTyper;
	}

	public void setKundhandelseTyper(final List<String> kundhandelseTyper) {
		this.kundhandelseTyper = kundhandelseTyper;
	}

	public Metadata withKundhandelseTyper(final List<String> kundhandelseTyper) {
		this.kundhandelseTyper = kundhandelseTyper;
		return this;
	}

	public List<String> getTaggar() {
		return taggar;
	}

	public void setTaggar(final List<String> taggar) {
		this.taggar = taggar;
	}

	public Metadata withTaggar(final List<String> taggar) {
		this.taggar = taggar;
		return this;
	}

	public String getStartDatum() {
		return startDatum;
	}

	public void setStartDatum(final String startDatum) {
		this.startDatum = startDatum;
	}

	public Metadata withStartDatum(final String startDatum) {
		this.startDatum = startDatum;
		return this;
	}

	public String getSlutDatum() {
		return slutDatum;
	}

	public void setSlutDatum(final String slutDatum) {
		this.slutDatum = slutDatum;
	}

	public Metadata withSlutDatum(final String slutDatum) {
		this.slutDatum = slutDatum;
		return this;
	}

	public String getOnskatSprak() {
		return onskatSprak;
	}

	public void setOnskatSprak(final String onskatSprak) {
		this.onskatSprak = onskatSprak;
	}

	public Metadata withOnskatSprak(final String onskatSprak) {
		this.onskatSprak = onskatSprak;
		return this;
	}

	public Behandling getBehandling() {
		return behandling;
	}

	public void setBehandling(final Behandling behandling) {
		this.behandling = behandling;
	}

	public Metadata withBehandling(final Behandling behandling) {
		this.behandling = behandling;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parter, kundhandelseTyper, taggar, startDatum, slutDatum, onskatSprak, behandling);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Metadata other)) {
			return false;
		}
		return Objects.equals(parter, other.parter) && Objects.equals(kundhandelseTyper, other.kundhandelseTyper) && Objects.equals(taggar, other.taggar) && Objects.equals(startDatum, other.startDatum) && Objects.equals(slutDatum, other.slutDatum)
			&& Objects.equals(onskatSprak, other.onskatSprak) && Objects.equals(behandling, other.behandling);
	}

	@Override
	public String toString() {
		return "Metadata [parter=" + parter +
			", kundhandelseTyper=" + kundhandelseTyper +
			", taggar=" + taggar +
			", startDatum=" + startDatum +
			", slutDatum=" + slutDatum +
			", onskatSprak=" + onskatSprak +
			", behandling=" + behandling +
			"]";
	}
}
