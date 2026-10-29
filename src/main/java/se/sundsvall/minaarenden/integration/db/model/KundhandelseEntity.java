package se.sundsvall.minaarenden.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.dept44.util.PiiMasker;

import static org.hibernate.Length.LONG32;

/**
 * En kundhändelse i ärendecachen. Fälten motsvarar objektet kundhandelse i API-specifikationen, kompletterat med
 * kommun och den part (kund och/eller ärende) som händelsen gäller. Kolumnnamnen följer API-specifikationen.
 */
@Entity
@Table(name = "kundhandelse",
	uniqueConstraints = @UniqueConstraint(name = KundhandelseEntity.UNIK_KUNDHANDELSE_ID, columnNames = {
		"municipality_id", "kundhandelse_id"
	}),
	indexes = {
		@Index(name = "ix_kundhandelse_kund", columnList = "municipality_id, kund_identifierare, kund_typ"),
		@Index(name = "ix_kundhandelse_arende", columnList = "municipality_id, arende_identifierare, arende_typ"),
		@Index(name = "ix_kundhandelse_tidpunkt", columnList = "tidpunkt"),
		@Index(name = "ix_kundhandelse_typ", columnList = "kundhandelse_typ")
	})
public class KundhandelseEntity implements Serializable {

	/** Det unika villkoret för kundhandelseId inom kommunen. */
	public static final String UNIK_KUNDHANDELSE_ID = "uq_kundhandelse_municipality_id_kundhandelse_id";

	@Serial
	private static final long serialVersionUID = 4034813525227427812L;

	@Id
	@UuidGenerator
	@Column(name = "id", nullable = false, length = 255)
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 4)
	private String municipalityId;

	@Column(name = "kundhandelse_id", nullable = false, length = 100)
	private String kundhandelseId;

	@Column(name = "producent", nullable = false, length = 200)
	private String producent;

	@Column(name = "kund_identifierare", length = 12)
	private String kundIdentifierare;

	@Column(name = "kund_typ", length = 20)
	private String kundTyp;

	@Column(name = "kund_tillagg", length = 20)
	private String kundTillagg;

	@Column(name = "arende_identifierare", length = 100)
	private String arendeIdentifierare;

	@Column(name = "arende_typ", length = 30)
	private String arendeTyp;

	@Column(name = "rubrik", nullable = false)
	private String rubrik;

	@Column(name = "beskrivning", nullable = false, length = LONG32)
	private String beskrivning;

	@Column(name = "sprak", nullable = false, length = 10)
	private String sprak;

	@Column(name = "tidpunkt", nullable = false)
	private Instant tidpunkt;

	@Column(name = "kundhandelse_typ", nullable = false, length = 200)
	private String kundhandelseTyp;

	@Column(name = "producentarendet_kraver_kundatgard", nullable = false)
	private boolean producentarendetKraverKundatgard;

	@Column(name = "producentarendet_klart", nullable = false)
	private boolean producentarendetKlart;

	@Column(name = "version", nullable = false, length = 10)
	private String version;

	@Column(name = "utokad_information", length = LONG32)
	private String utokadInformation;

	@Column(name = "referenser", length = LONG32)
	private String referenser;

	@Column(name = "skapad", nullable = false)
	private Instant skapad;

	@Column(name = "andrad")
	private Instant andrad;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "kundhandelse_tagg",
		joinColumns = @JoinColumn(name = "kundhandelse_ref"),
		foreignKey = @ForeignKey(name = "fk_tagg_kundhandelse"),
		indexes = @Index(name = "ix_kundhandelse_tagg", columnList = "tagg"))
	@Column(name = "tagg", nullable = false, length = 100)
	private Set<String> taggar = new LinkedHashSet<>();

	public static KundhandelseEntity create() {
		return new KundhandelseEntity();
	}

	@PrePersist
	void prePersist() {
		skapad = Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}

	@PreUpdate
	void preUpdate() {
		andrad = Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public KundhandelseEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public KundhandelseEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getKundhandelseId() {
		return kundhandelseId;
	}

	public void setKundhandelseId(final String kundhandelseId) {
		this.kundhandelseId = kundhandelseId;
	}

	public KundhandelseEntity withKundhandelseId(final String kundhandelseId) {
		this.kundhandelseId = kundhandelseId;
		return this;
	}

	public String getProducent() {
		return producent;
	}

	public void setProducent(final String producent) {
		this.producent = producent;
	}

	public KundhandelseEntity withProducent(final String producent) {
		this.producent = producent;
		return this;
	}

	public String getKundIdentifierare() {
		return kundIdentifierare;
	}

	public void setKundIdentifierare(final String kundIdentifierare) {
		this.kundIdentifierare = kundIdentifierare;
	}

	public KundhandelseEntity withKundIdentifierare(final String kundIdentifierare) {
		this.kundIdentifierare = kundIdentifierare;
		return this;
	}

	public String getKundTyp() {
		return kundTyp;
	}

	public void setKundTyp(final String kundTyp) {
		this.kundTyp = kundTyp;
	}

	public KundhandelseEntity withKundTyp(final String kundTyp) {
		this.kundTyp = kundTyp;
		return this;
	}

	public String getKundTillagg() {
		return kundTillagg;
	}

	public void setKundTillagg(final String kundTillagg) {
		this.kundTillagg = kundTillagg;
	}

	public KundhandelseEntity withKundTillagg(final String kundTillagg) {
		this.kundTillagg = kundTillagg;
		return this;
	}

	public String getArendeIdentifierare() {
		return arendeIdentifierare;
	}

	public void setArendeIdentifierare(final String arendeIdentifierare) {
		this.arendeIdentifierare = arendeIdentifierare;
	}

	public KundhandelseEntity withArendeIdentifierare(final String arendeIdentifierare) {
		this.arendeIdentifierare = arendeIdentifierare;
		return this;
	}

	public String getArendeTyp() {
		return arendeTyp;
	}

	public void setArendeTyp(final String arendeTyp) {
		this.arendeTyp = arendeTyp;
	}

	public KundhandelseEntity withArendeTyp(final String arendeTyp) {
		this.arendeTyp = arendeTyp;
		return this;
	}

	public String getRubrik() {
		return rubrik;
	}

	public void setRubrik(final String rubrik) {
		this.rubrik = rubrik;
	}

	public KundhandelseEntity withRubrik(final String rubrik) {
		this.rubrik = rubrik;
		return this;
	}

	public String getBeskrivning() {
		return beskrivning;
	}

	public void setBeskrivning(final String beskrivning) {
		this.beskrivning = beskrivning;
	}

	public KundhandelseEntity withBeskrivning(final String beskrivning) {
		this.beskrivning = beskrivning;
		return this;
	}

	public String getSprak() {
		return sprak;
	}

	public void setSprak(final String sprak) {
		this.sprak = sprak;
	}

	public KundhandelseEntity withSprak(final String sprak) {
		this.sprak = sprak;
		return this;
	}

	public Instant getTidpunkt() {
		return tidpunkt;
	}

	public void setTidpunkt(final Instant tidpunkt) {
		this.tidpunkt = tidpunkt;
	}

	public KundhandelseEntity withTidpunkt(final Instant tidpunkt) {
		this.tidpunkt = tidpunkt;
		return this;
	}

	public String getKundhandelseTyp() {
		return kundhandelseTyp;
	}

	public void setKundhandelseTyp(final String kundhandelseTyp) {
		this.kundhandelseTyp = kundhandelseTyp;
	}

	public KundhandelseEntity withKundhandelseTyp(final String kundhandelseTyp) {
		this.kundhandelseTyp = kundhandelseTyp;
		return this;
	}

	public boolean isProducentarendetKraverKundatgard() {
		return producentarendetKraverKundatgard;
	}

	public void setProducentarendetKraverKundatgard(final boolean producentarendetKraverKundatgard) {
		this.producentarendetKraverKundatgard = producentarendetKraverKundatgard;
	}

	public KundhandelseEntity withProducentarendetKraverKundatgard(final boolean producentarendetKraverKundatgard) {
		this.producentarendetKraverKundatgard = producentarendetKraverKundatgard;
		return this;
	}

	public boolean isProducentarendetKlart() {
		return producentarendetKlart;
	}

	public void setProducentarendetKlart(final boolean producentarendetKlart) {
		this.producentarendetKlart = producentarendetKlart;
	}

	public KundhandelseEntity withProducentarendetKlart(final boolean producentarendetKlart) {
		this.producentarendetKlart = producentarendetKlart;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public KundhandelseEntity withVersion(final String version) {
		this.version = version;
		return this;
	}

	public String getUtokadInformation() {
		return utokadInformation;
	}

	public void setUtokadInformation(final String utokadInformation) {
		this.utokadInformation = utokadInformation;
	}

	public KundhandelseEntity withUtokadInformation(final String utokadInformation) {
		this.utokadInformation = utokadInformation;
		return this;
	}

	public String getReferenser() {
		return referenser;
	}

	public void setReferenser(final String referenser) {
		this.referenser = referenser;
	}

	public KundhandelseEntity withReferenser(final String referenser) {
		this.referenser = referenser;
		return this;
	}

	public Instant getSkapad() {
		return skapad;
	}

	public void setSkapad(final Instant skapad) {
		this.skapad = skapad;
	}

	public KundhandelseEntity withSkapad(final Instant skapad) {
		this.skapad = skapad;
		return this;
	}

	public Instant getAndrad() {
		return andrad;
	}

	public void setAndrad(final Instant andrad) {
		this.andrad = andrad;
	}

	public KundhandelseEntity withAndrad(final Instant andrad) {
		this.andrad = andrad;
		return this;
	}

	public Set<String> getTaggar() {
		return taggar;
	}

	public void setTaggar(final Set<String> taggar) {
		this.taggar = taggar == null ? new LinkedHashSet<>() : new LinkedHashSet<>(taggar);
	}

	public KundhandelseEntity withTaggar(final Set<String> taggar) {
		setTaggar(taggar);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(andrad, arendeIdentifierare, arendeTyp, beskrivning, id, kundIdentifierare, kundTillagg, kundTyp, kundhandelseId, kundhandelseTyp, municipalityId, producent,
			producentarendetKlart, producentarendetKraverKundatgard, referenser, rubrik, skapad, sprak, taggar, tidpunkt, utokadInformation, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final KundhandelseEntity other)) {
			return false;
		}
		return Objects.equals(andrad, other.andrad) && Objects.equals(arendeIdentifierare, other.arendeIdentifierare) && Objects.equals(arendeTyp, other.arendeTyp)
			&& Objects.equals(beskrivning, other.beskrivning) && Objects.equals(id, other.id) && Objects.equals(kundIdentifierare, other.kundIdentifierare)
			&& Objects.equals(kundTillagg, other.kundTillagg) && Objects.equals(kundTyp, other.kundTyp) && Objects.equals(kundhandelseId, other.kundhandelseId)
			&& Objects.equals(kundhandelseTyp, other.kundhandelseTyp) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(producent, other.producent)
			&& (producentarendetKlart == other.producentarendetKlart) && (producentarendetKraverKundatgard == other.producentarendetKraverKundatgard)
			&& Objects.equals(referenser, other.referenser) && Objects.equals(rubrik, other.rubrik) && Objects.equals(skapad, other.skapad) && Objects.equals(sprak, other.sprak)
			&& Objects.equals(taggar, other.taggar) && Objects.equals(tidpunkt, other.tidpunkt) && Objects.equals(utokadInformation, other.utokadInformation)
			&& Objects.equals(version, other.version);
	}

	/**
	 * Kundens identifierare (personnummer m.m.) maskeras, så att entiteten kan loggas.
	 */
	@Override
	public String toString() {
		return new StringBuilder()
			.append("KundhandelseEntity [id=").append(id)
			.append(", municipalityId=").append(municipalityId)
			.append(", kundhandelseId=").append(kundhandelseId)
			.append(", producent=").append(producent)
			.append(", kundIdentifierare=").append(PiiMasker.maskPersonalNumber(kundIdentifierare))
			.append(", kundTyp=").append(kundTyp)
			.append(", kundTillagg=").append(kundTillagg)
			.append(", arendeIdentifierare=").append(arendeIdentifierare)
			.append(", arendeTyp=").append(arendeTyp)
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
			.append(", skapad=").append(skapad)
			.append(", andrad=").append(andrad)
			.append(", taggar=").append(taggar)
			.append("]").toString();
	}
}
