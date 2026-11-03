package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

@Schema(description = "En kundhändelse enligt Mina ärendens standard")
public class Kundhandelse {

	@Schema(description = "Kundhändelsens id, satt av producenten och unikt inom kommunen", examples = "REFKOM-BYGG-2026-00123-1")
	private String kundhandelseId;

	@Schema(description = "Kort rubrik för kundhändelsen", examples = "Ansökan om bygglov mottagen")
	private String rubrik;

	@Schema(description = "Beskrivning av kundhändelsen för kunden", examples = "Vi har tagit emot din ansökan om bygglov.")
	private String beskrivning;

	@Schema(description = "Språket i rubrik och beskrivning enligt ISO 639-1", examples = "sv")
	private String sprak;

	@Schema(description = "Namnet på producenten som kundhändelsen kommer från", examples = "Referenskommunen")
	private String producent;

	@Schema(description = "Tidpunkt för händelsen enligt RFC 3339, i svensk tid", examples = "2026-09-01T10:15:00+02:00")
	private String tidpunkt;

	@Schema(description = "Typ av kundhändelse i formatet PRODUCENTPREFIX.ARENDETYP.HANDELSETYP", examples = "REFKOM.BYGGLOV.ANSOKAN_MOTTAGEN")
	private String kundhandelseTyp;

	@Schema(description = "Om producentärendet väntar på en åtgärd från kunden")
	private boolean producentarendetKraverKundatgard;

	@Schema(description = "Om producentärendet är avslutat")
	private boolean producentarendetKlart;

	@Schema(description = "Version av standarden som producenten implementerat", examples = "6.1")
	private String version;

	@Schema(types = "object", implementation = Object.class, description = "Utökad information (fritt JSON-objekt)")
	private JsonNode utokadInformation;

	@Schema(types = "object", implementation = Object.class, description = "Referenser (fritt JSON-objekt)")
	private JsonNode referenser;

	public static Kundhandelse create() {
		return new Kundhandelse();
	}

	public String getKundhandelseId() {
		return kundhandelseId;
	}

	public void setKundhandelseId(final String kundhandelseId) {
		this.kundhandelseId = kundhandelseId;
	}

	public Kundhandelse withKundhandelseId(final String kundhandelseId) {
		this.kundhandelseId = kundhandelseId;
		return this;
	}

	public String getRubrik() {
		return rubrik;
	}

	public void setRubrik(final String rubrik) {
		this.rubrik = rubrik;
	}

	public Kundhandelse withRubrik(final String rubrik) {
		this.rubrik = rubrik;
		return this;
	}

	public String getBeskrivning() {
		return beskrivning;
	}

	public void setBeskrivning(final String beskrivning) {
		this.beskrivning = beskrivning;
	}

	public Kundhandelse withBeskrivning(final String beskrivning) {
		this.beskrivning = beskrivning;
		return this;
	}

	public String getSprak() {
		return sprak;
	}

	public void setSprak(final String sprak) {
		this.sprak = sprak;
	}

	public Kundhandelse withSprak(final String sprak) {
		this.sprak = sprak;
		return this;
	}

	public String getProducent() {
		return producent;
	}

	public void setProducent(final String producent) {
		this.producent = producent;
	}

	public Kundhandelse withProducent(final String producent) {
		this.producent = producent;
		return this;
	}

	public String getTidpunkt() {
		return tidpunkt;
	}

	public void setTidpunkt(final String tidpunkt) {
		this.tidpunkt = tidpunkt;
	}

	public Kundhandelse withTidpunkt(final String tidpunkt) {
		this.tidpunkt = tidpunkt;
		return this;
	}

	public String getKundhandelseTyp() {
		return kundhandelseTyp;
	}

	public void setKundhandelseTyp(final String kundhandelseTyp) {
		this.kundhandelseTyp = kundhandelseTyp;
	}

	public Kundhandelse withKundhandelseTyp(final String kundhandelseTyp) {
		this.kundhandelseTyp = kundhandelseTyp;
		return this;
	}

	public boolean isProducentarendetKraverKundatgard() {
		return producentarendetKraverKundatgard;
	}

	public void setProducentarendetKraverKundatgard(final boolean producentarendetKraverKundatgard) {
		this.producentarendetKraverKundatgard = producentarendetKraverKundatgard;
	}

	public Kundhandelse withProducentarendetKraverKundatgard(final boolean producentarendetKraverKundatgard) {
		this.producentarendetKraverKundatgard = producentarendetKraverKundatgard;
		return this;
	}

	public boolean isProducentarendetKlart() {
		return producentarendetKlart;
	}

	public void setProducentarendetKlart(final boolean producentarendetKlart) {
		this.producentarendetKlart = producentarendetKlart;
	}

	public Kundhandelse withProducentarendetKlart(final boolean producentarendetKlart) {
		this.producentarendetKlart = producentarendetKlart;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public Kundhandelse withVersion(final String version) {
		this.version = version;
		return this;
	}

	public JsonNode getUtokadInformation() {
		return utokadInformation;
	}

	public void setUtokadInformation(final JsonNode utokadInformation) {
		this.utokadInformation = utokadInformation;
	}

	public Kundhandelse withUtokadInformation(final JsonNode utokadInformation) {
		this.utokadInformation = utokadInformation;
		return this;
	}

	public JsonNode getReferenser() {
		return referenser;
	}

	public void setReferenser(final JsonNode referenser) {
		this.referenser = referenser;
	}

	public Kundhandelse withReferenser(final JsonNode referenser) {
		this.referenser = referenser;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(kundhandelseId, rubrik, beskrivning, sprak, producent, tidpunkt, kundhandelseTyp, producentarendetKraverKundatgard, producentarendetKlart, version, utokadInformation, referenser);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Kundhandelse other)) {
			return false;
		}
		return Objects.equals(kundhandelseId, other.kundhandelseId) && Objects.equals(rubrik, other.rubrik) && Objects.equals(beskrivning, other.beskrivning) && Objects.equals(sprak, other.sprak) && Objects.equals(producent, other.producent) && Objects
			.equals(tidpunkt, other.tidpunkt) && Objects.equals(kundhandelseTyp, other.kundhandelseTyp) && (producentarendetKraverKundatgard == other.producentarendetKraverKundatgard) && (producentarendetKlart == other.producentarendetKlart) && Objects
				.equals(version, other.version) && Objects.equals(utokadInformation, other.utokadInformation) && Objects.equals(referenser, other.referenser);
	}

	@Override
	public String toString() {
		return "Kundhandelse [kundhandelseId=" + kundhandelseId +
			", rubrik=" + rubrik +
			", beskrivning=" + beskrivning +
			", sprak=" + sprak +
			", producent=" + producent +
			", tidpunkt=" + tidpunkt +
			", kundhandelseTyp=" + kundhandelseTyp +
			", producentarendetKraverKundatgard=" + producentarendetKraverKundatgard +
			", producentarendetKlart=" + producentarendetKlart +
			", version=" + version +
			", utokadInformation=" + utokadInformation +
			", referenser=" + referenser +
			"]";
	}
}
