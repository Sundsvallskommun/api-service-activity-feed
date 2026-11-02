package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import se.sundsvall.activityfeed.api.validation.ActivityTypeFormat;
import se.sundsvall.activityfeed.api.validation.Limits;
import tools.jackson.databind.JsonNode;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Ingest into the activity cache: an activity and the party it concerns. The length limits match the columns in the
 * activity table, so an error gives a 400 that names the field instead of a database error.
 */
@Schema(description = "Kundhändelse att lägga i ärendecachen. producent, sprak och version fylls i från konfigurationen om de utelämnas.")
public class NyKundhandelse {

	@Schema(examples = "REFKOM-BYGG-2026-00123-3", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 100)
	private String kundhandelseId;

	@Schema(requiredMode = REQUIRED)
	@NotNull
	@Valid
	private Part part;

	@Schema(examples = "Beslut om bygglov", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String rubrik;

	@Schema(examples = "Bygglov har beviljats. Beslutet finns på Mina sidor.", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 65535)
	private String beskrivning;

	@Schema(description = "ISO 639-1, standard sv", examples = "sv")
	@Pattern(regexp = "^[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$", message = "sprak ska vara en språkkod enligt ISO 639-1, t.ex. sv")
	@Size(max = 10)
	private String sprak;

	@Schema(description = "RFC 3339, eller lokal tid ÅÅÅÅ-MM-DD HH:MM:SS i Europe/Stockholm", examples = "2026-09-20T10:00:00+02:00", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 40)
	private String tidpunkt;

	@Schema(description = "PRODUCENTPREFIX.ARENDETYP.HANDELSETYP", examples = "REFKOM.BYGGLOV.BESLUT", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = ActivityTypeFormat.MAX_LENGTH)
	@Pattern(regexp = ActivityTypeFormat.INGEST_PATTERN, message = "kundhandelseTyp ska ha formatet PRODUCENT.ARENDETYP.HANDELSETYP (versaler, siffror, understreck)")
	private String kundhandelseTyp;

	@Schema(requiredMode = REQUIRED)
	@NotNull
	private Boolean producentarendetKraverKundatgard;

	@Schema(requiredMode = REQUIRED)
	@NotNull
	private Boolean producentarendetKlart;

	@Schema(description = "Version av standarden, standard enligt konfigurationen", examples = "6.1")
	@Pattern(regexp = "^\\d+(?:\\.\\d+)*$", message = "version ska anges som t.ex. 6.1")
	@Size(max = 10)
	private String version;

	@Schema(types = "object", implementation = Object.class, description = "Utökad information (fritt JSON-objekt)")
	private JsonNode utokadInformation;

	@Schema(types = "object", implementation = Object.class, description = "Referenser (fritt JSON-objekt)")
	private JsonNode referenser;

	@Schema(description = "Taggar som frågor kan filtrera på", examples = "bygglov")
	@Size(max = Limits.MAX_TAGS, message = "taggar får innehålla högst " + Limits.MAX_TAGS + " taggar")
	private List<@NotBlank(message = "taggar får inte innehålla tomma värden") @Size(max = Limits.MAX_TAG_LENGTH, message = "taggar: för lång tagg") String> taggar;

	public static NyKundhandelse create() {
		return new NyKundhandelse();
	}

	public String getKundhandelseId() {
		return kundhandelseId;
	}

	public void setKundhandelseId(final String kundhandelseId) {
		this.kundhandelseId = kundhandelseId;
	}

	public NyKundhandelse withKundhandelseId(final String kundhandelseId) {
		this.kundhandelseId = kundhandelseId;
		return this;
	}

	public Part getPart() {
		return part;
	}

	public void setPart(final Part part) {
		this.part = part;
	}

	public NyKundhandelse withPart(final Part part) {
		this.part = part;
		return this;
	}

	public String getRubrik() {
		return rubrik;
	}

	public void setRubrik(final String rubrik) {
		this.rubrik = rubrik;
	}

	public NyKundhandelse withRubrik(final String rubrik) {
		this.rubrik = rubrik;
		return this;
	}

	public String getBeskrivning() {
		return beskrivning;
	}

	public void setBeskrivning(final String beskrivning) {
		this.beskrivning = beskrivning;
	}

	public NyKundhandelse withBeskrivning(final String beskrivning) {
		this.beskrivning = beskrivning;
		return this;
	}

	public String getSprak() {
		return sprak;
	}

	public void setSprak(final String sprak) {
		this.sprak = sprak;
	}

	public NyKundhandelse withSprak(final String sprak) {
		this.sprak = sprak;
		return this;
	}

	public String getTidpunkt() {
		return tidpunkt;
	}

	public void setTidpunkt(final String tidpunkt) {
		this.tidpunkt = tidpunkt;
	}

	public NyKundhandelse withTidpunkt(final String tidpunkt) {
		this.tidpunkt = tidpunkt;
		return this;
	}

	public String getKundhandelseTyp() {
		return kundhandelseTyp;
	}

	public void setKundhandelseTyp(final String kundhandelseTyp) {
		this.kundhandelseTyp = kundhandelseTyp;
	}

	public NyKundhandelse withKundhandelseTyp(final String kundhandelseTyp) {
		this.kundhandelseTyp = kundhandelseTyp;
		return this;
	}

	public Boolean getProducentarendetKraverKundatgard() {
		return producentarendetKraverKundatgard;
	}

	public void setProducentarendetKraverKundatgard(final Boolean producentarendetKraverKundatgard) {
		this.producentarendetKraverKundatgard = producentarendetKraverKundatgard;
	}

	public NyKundhandelse withProducentarendetKraverKundatgard(final Boolean producentarendetKraverKundatgard) {
		this.producentarendetKraverKundatgard = producentarendetKraverKundatgard;
		return this;
	}

	public Boolean getProducentarendetKlart() {
		return producentarendetKlart;
	}

	public void setProducentarendetKlart(final Boolean producentarendetKlart) {
		this.producentarendetKlart = producentarendetKlart;
	}

	public NyKundhandelse withProducentarendetKlart(final Boolean producentarendetKlart) {
		this.producentarendetKlart = producentarendetKlart;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public NyKundhandelse withVersion(final String version) {
		this.version = version;
		return this;
	}

	public JsonNode getUtokadInformation() {
		return utokadInformation;
	}

	public void setUtokadInformation(final JsonNode utokadInformation) {
		this.utokadInformation = utokadInformation;
	}

	public NyKundhandelse withUtokadInformation(final JsonNode utokadInformation) {
		this.utokadInformation = utokadInformation;
		return this;
	}

	public JsonNode getReferenser() {
		return referenser;
	}

	public void setReferenser(final JsonNode referenser) {
		this.referenser = referenser;
	}

	public NyKundhandelse withReferenser(final JsonNode referenser) {
		this.referenser = referenser;
		return this;
	}

	public List<String> getTaggar() {
		return taggar;
	}

	public void setTaggar(final List<String> taggar) {
		this.taggar = taggar;
	}

	public NyKundhandelse withTaggar(final List<String> taggar) {
		this.taggar = taggar;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(kundhandelseId, part, rubrik, beskrivning, sprak, tidpunkt, kundhandelseTyp, producentarendetKraverKundatgard, producentarendetKlart, version, utokadInformation, referenser, taggar);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final NyKundhandelse other)) {
			return false;
		}
		return Objects.equals(kundhandelseId, other.kundhandelseId) && Objects.equals(part, other.part) && Objects.equals(rubrik, other.rubrik) && Objects.equals(beskrivning, other.beskrivning) && Objects.equals(sprak, other.sprak) && Objects.equals(
			tidpunkt, other.tidpunkt) && Objects.equals(kundhandelseTyp, other.kundhandelseTyp) && Objects.equals(producentarendetKraverKundatgard, other.producentarendetKraverKundatgard) && Objects.equals(producentarendetKlart,
				other.producentarendetKlart) && Objects.equals(version, other.version) && Objects.equals(utokadInformation, other.utokadInformation) && Objects.equals(referenser, other.referenser) && Objects.equals(taggar, other.taggar);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("NyKundhandelse [kundhandelseId=").append(kundhandelseId)
			.append(", part=").append(part)
			.append(", rubrik=").append(rubrik)
			.append(", beskrivning=").append(beskrivning)
			.append(", sprak=").append(sprak)
			.append(", tidpunkt=").append(tidpunkt)
			.append(", kundhandelseTyp=").append(kundhandelseTyp)
			.append(", producentarendetKraverKundatgard=").append(producentarendetKraverKundatgard)
			.append(", producentarendetKlart=").append(producentarendetKlart)
			.append(", version=").append(version)
			.append(", utokadInformation=").append(utokadInformation)
			.append(", referenser=").append(referenser)
			.append(", taggar=").append(taggar)
			.append("]").toString();
	}
}
