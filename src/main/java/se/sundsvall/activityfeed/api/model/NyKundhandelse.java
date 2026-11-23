package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Ingest into the activity cache: an activity and the party it concerns. The length limits match the columns in the
 * activity table, so an error gives a 400 that names the field instead of a database error.
 *
 * <p>
 * The patterns use possessive quantifiers (++, *+). Bean Validation runs the pattern on the whole value, whatever its
 * length, and a repeated group in a Java regex recurses once per repetition, so a long value would otherwise overflow
 * the stack.
 */
@Schema(description = "Kundhändelse att lägga i ärendecachen. producent, sprak och version fylls i från konfigurationen om de utelämnas.")
public class NyKundhandelse {

	@Schema(description = "Kundhändelsens id, satt av producenten och unikt inom kommunen. Samma id ersätter en befintlig kundhändelse.",
		examples = "REFKOM-BYGG-2026-00123-3",
		requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 100)
	private String kundhandelseId;

	@Schema(description = "Kunden, ärendet eller båda som kundhändelsen gäller", requiredMode = REQUIRED)
	@NotNull
	@Valid
	private Part part;

	@Schema(description = "Kort rubrik för kundhändelsen", examples = "Beslut om bygglov", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String rubrik;

	@Schema(description = "Beskrivning av kundhändelsen för kunden", examples = "Bygglov har beviljats. Beslutet finns på Mina sidor.", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 65535)
	private String beskrivning;

	@Schema(description = "Språket i rubrik och beskrivning enligt ISO 639-1. Utan värde blir det sv.", examples = "sv")
	@Pattern(regexp = "^[a-z]{2,3}(?:-[A-Za-z0-9]{2,8}+)*+$", message = "sprak ska vara en språkkod enligt ISO 639-1, t.ex. sv")
	@Size(max = 10)
	private String sprak;

	@Schema(description = "Tidpunkt för händelsen enligt RFC 3339", examples = "2026-09-20T10:00:00+02:00", requiredMode = REQUIRED)
	@NotNull
	private OffsetDateTime tidpunkt;

	@Schema(description = "Typ av kundhändelse i formatet PRODUCENTPREFIX.ARENDETYP.HANDELSETYP, där prefixet är kommunens. Minst tre delar åtskilda av punkt. "
		+ "Prefixet får innehålla A-Z, övriga delar A-Z, 0-9 och _.",
		examples = "REFKOM.BYGGLOV.BESLUT",
		requiredMode = REQUIRED)
	@NotBlank
	// Strict for the producer's own data: at least three parts (producer, producer errand type, activity type), no empty
	// parts. The length matches the column activity_type.
	@Size(max = 200)
	@Pattern(regexp = "^[A-Z]++(?:\\.[A-Z0-9_]++){2,}+$", message = "kundhandelseTyp ska bestå av minst tre delar åtskilda av punkt, t.ex. REFKOM.BYGGLOV.BESLUT. Prefixet får innehålla A-Z, övriga delar A-Z, 0-9 och _")
	private String kundhandelseTyp;

	@Schema(description = "Om producentärendet väntar på en åtgärd från kunden", requiredMode = REQUIRED)
	@NotNull
	private Boolean producentarendetKraverKundatgard;

	@Schema(description = "Om producentärendet är avslutat", requiredMode = REQUIRED)
	@NotNull
	private Boolean producentarendetKlart;

	@Schema(description = "Version av Mina ärendens standard. Utan värde blir det den konfigurerade versionen.", examples = "6.1")
	@Pattern(regexp = "^\\d++(?:\\.\\d++)*+$", message = "version ska anges som t.ex. 6.1")
	@Size(max = 10, message = "version får vara högst {max} tecken")
	private String version;

	@Schema(types = "object", implementation = Object.class, description = "Utökad information (fritt JSON-objekt)")
	private JsonNode utokadInformation;

	@Schema(types = "object", implementation = Object.class, description = "Referenser (fritt JSON-objekt)")
	private JsonNode referenser;

	@Schema(description = "Taggar som frågor kan filtrera på", examples = "bygglov")
	@Size(max = 100, message = "taggar får innehålla högst {max} taggar")
	private List<@NotBlank(message = "taggar får inte innehålla tomma värden") @Size(max = 100, message = "taggar: för lång tagg") String> taggar;

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

	public OffsetDateTime getTidpunkt() {
		return tidpunkt;
	}

	public void setTidpunkt(final OffsetDateTime tidpunkt) {
		this.tidpunkt = tidpunkt;
	}

	public NyKundhandelse withTidpunkt(final OffsetDateTime tidpunkt) {
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
		return "NyKundhandelse [kundhandelseId=" + kundhandelseId +
			", part=" + part +
			", rubrik=" + rubrik +
			", beskrivning=" + beskrivning +
			", sprak=" + sprak +
			", tidpunkt=" + tidpunkt +
			", kundhandelseTyp=" + kundhandelseTyp +
			", producentarendetKraverKundatgard=" + producentarendetKraverKundatgard +
			", producentarendetKlart=" + producentarendetKlart +
			", version=" + version +
			", utokadInformation=" + utokadInformation +
			", referenser=" + referenser +
			", taggar=" + taggar +
			"]";
	}
}
