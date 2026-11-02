package se.sundsvall.activityfeed.integration.db.model;

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
import java.util.Optional;
import java.util.Set;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.dept44.util.PiiMasker;

import static org.hibernate.Length.LONG32;

/**
 * An activity (kundhändelse) in the activity cache. The fields match the kundhandelse object in the API specification,
 * plus the municipality and the party (customer and/or errand) the activity concerns.
 */
@Entity
@Table(name = "activity",
	uniqueConstraints = @UniqueConstraint(name = ActivityEntity.UNIQUE_ACTIVITY_ID, columnNames = {
		"municipality_id", "activity_id"
	}),
	indexes = {
		@Index(name = "ix_activity_customer", columnList = "municipality_id, customer_identifier, customer_type"),
		@Index(name = "ix_activity_errand", columnList = "municipality_id, errand_identifier, errand_type"),
		@Index(name = "ix_activity_occurred_at", columnList = "occurred_at"),
		@Index(name = "ix_activity_type", columnList = "activity_type")
	})
public class ActivityEntity implements Serializable {

	/** The unique constraint on activityId within the municipality. */
	public static final String UNIQUE_ACTIVITY_ID = "uq_activity_municipality_id_activity_id";

	@Serial
	private static final long serialVersionUID = 4034813525227427812L;

	@Id
	@UuidGenerator
	@Column(name = "id", nullable = false, length = 255)
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 4)
	private String municipalityId;

	@Column(name = "activity_id", nullable = false, length = 100)
	private String activityId;

	@Column(name = "producer", nullable = false, length = 200)
	private String producer;

	@Column(name = "customer_identifier", length = 12)
	private String customerIdentifier;

	@Column(name = "customer_type", length = 20)
	private String customerType;

	@Column(name = "customer_suffix", length = 20)
	private String customerSuffix;

	@Column(name = "errand_identifier", length = 100)
	private String errandIdentifier;

	@Column(name = "errand_type", length = 30)
	private String errandType;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "description", nullable = false, length = LONG32)
	private String description;

	@Column(name = "language", nullable = false, length = 10)
	private String language;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "activity_type", nullable = false, length = 200)
	private String activityType;

	@Column(name = "customer_action_required", nullable = false)
	private boolean customerActionRequired;

	@Column(name = "errand_completed", nullable = false)
	private boolean errandCompleted;

	@Column(name = "version", nullable = false, length = 10)
	private String version;

	@Column(name = "extended_information", length = LONG32)
	private String extendedInformation;

	@Column(name = "reference_data", length = LONG32)
	private String referenceData;

	@Column(name = "created", nullable = false)
	private Instant created;

	@Column(name = "modified")
	private Instant modified;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "activity_tag",
		joinColumns = @JoinColumn(name = "activity_ref"),
		foreignKey = @ForeignKey(name = "fk_activity_tag_activity"),
		indexes = @Index(name = "ix_activity_tag", columnList = "tag"))
	@Column(name = "tag", nullable = false, length = 100)
	private Set<String> tags = new LinkedHashSet<>();

	public static ActivityEntity create() {
		return new ActivityEntity();
	}

	@PrePersist
	void prePersist() {
		created = Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}

	@PreUpdate
	void preUpdate() {
		modified = Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ActivityEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ActivityEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(final String activityId) {
		this.activityId = activityId;
	}

	public ActivityEntity withActivityId(final String activityId) {
		this.activityId = activityId;
		return this;
	}

	public String getProducer() {
		return producer;
	}

	public void setProducer(final String producer) {
		this.producer = producer;
	}

	public ActivityEntity withProducer(final String producer) {
		this.producer = producer;
		return this;
	}

	public String getCustomerIdentifier() {
		return customerIdentifier;
	}

	public void setCustomerIdentifier(final String customerIdentifier) {
		this.customerIdentifier = customerIdentifier;
	}

	public ActivityEntity withCustomerIdentifier(final String customerIdentifier) {
		this.customerIdentifier = customerIdentifier;
		return this;
	}

	public String getCustomerType() {
		return customerType;
	}

	public void setCustomerType(final String customerType) {
		this.customerType = customerType;
	}

	public ActivityEntity withCustomerType(final String customerType) {
		this.customerType = customerType;
		return this;
	}

	public String getCustomerSuffix() {
		return customerSuffix;
	}

	public void setCustomerSuffix(final String customerSuffix) {
		this.customerSuffix = customerSuffix;
	}

	public ActivityEntity withCustomerSuffix(final String customerSuffix) {
		this.customerSuffix = customerSuffix;
		return this;
	}

	public String getErrandIdentifier() {
		return errandIdentifier;
	}

	public void setErrandIdentifier(final String errandIdentifier) {
		this.errandIdentifier = errandIdentifier;
	}

	public ActivityEntity withErrandIdentifier(final String errandIdentifier) {
		this.errandIdentifier = errandIdentifier;
		return this;
	}

	public String getErrandType() {
		return errandType;
	}

	public void setErrandType(final String errandType) {
		this.errandType = errandType;
	}

	public ActivityEntity withErrandType(final String errandType) {
		this.errandType = errandType;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public ActivityEntity withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public ActivityEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public ActivityEntity withLanguage(final String language) {
		this.language = language;
		return this;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(final Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public ActivityEntity withOccurredAt(final Instant occurredAt) {
		this.occurredAt = occurredAt;
		return this;
	}

	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(final String activityType) {
		this.activityType = activityType;
	}

	public ActivityEntity withActivityType(final String activityType) {
		this.activityType = activityType;
		return this;
	}

	public boolean isCustomerActionRequired() {
		return customerActionRequired;
	}

	public void setCustomerActionRequired(final boolean customerActionRequired) {
		this.customerActionRequired = customerActionRequired;
	}

	public ActivityEntity withCustomerActionRequired(final boolean customerActionRequired) {
		this.customerActionRequired = customerActionRequired;
		return this;
	}

	public boolean isErrandCompleted() {
		return errandCompleted;
	}

	public void setErrandCompleted(final boolean errandCompleted) {
		this.errandCompleted = errandCompleted;
	}

	public ActivityEntity withErrandCompleted(final boolean errandCompleted) {
		this.errandCompleted = errandCompleted;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public ActivityEntity withVersion(final String version) {
		this.version = version;
		return this;
	}

	public String getExtendedInformation() {
		return extendedInformation;
	}

	public void setExtendedInformation(final String extendedInformation) {
		this.extendedInformation = extendedInformation;
	}

	public ActivityEntity withExtendedInformation(final String extendedInformation) {
		this.extendedInformation = extendedInformation;
		return this;
	}

	public String getReferenceData() {
		return referenceData;
	}

	public void setReferenceData(final String referenceData) {
		this.referenceData = referenceData;
	}

	public ActivityEntity withReferenceData(final String referenceData) {
		this.referenceData = referenceData;
		return this;
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(final Instant created) {
		this.created = created;
	}

	public ActivityEntity withCreated(final Instant created) {
		this.created = created;
		return this;
	}

	public Instant getModified() {
		return modified;
	}

	public void setModified(final Instant modified) {
		this.modified = modified;
	}

	public ActivityEntity withModified(final Instant modified) {
		this.modified = modified;
		return this;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(final Set<String> tags) {
		this.tags = Optional.ofNullable(tags).map(LinkedHashSet::new).orElseGet(LinkedHashSet::new);
	}

	public ActivityEntity withTags(final Set<String> tags) {
		setTags(tags);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(activityId, activityType, created, customerActionRequired, customerIdentifier, customerSuffix, customerType, description, errandCompleted, errandIdentifier,
			errandType, extendedInformation, id, language, modified, municipalityId, occurredAt, producer, referenceData, tags, title, version);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ActivityEntity other)) {
			return false;
		}
		return Objects.equals(activityId, other.activityId) && Objects.equals(activityType, other.activityType) && Objects.equals(created, other.created)
			&& (customerActionRequired == other.customerActionRequired) && Objects.equals(customerIdentifier, other.customerIdentifier)
			&& Objects.equals(customerSuffix, other.customerSuffix) && Objects.equals(customerType, other.customerType) && Objects.equals(description, other.description)
			&& (errandCompleted == other.errandCompleted) && Objects.equals(errandIdentifier, other.errandIdentifier) && Objects.equals(errandType, other.errandType)
			&& Objects.equals(extendedInformation, other.extendedInformation) && Objects.equals(id, other.id) && Objects.equals(language, other.language)
			&& Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(occurredAt, other.occurredAt)
			&& Objects.equals(producer, other.producer) && Objects.equals(referenceData, other.referenceData) && Objects.equals(tags, other.tags)
			&& Objects.equals(title, other.title) && Objects.equals(version, other.version);
	}

	/**
	 * Masks the customer identifier (personal identity number etc.), so the entity can be logged.
	 */
	@Override
	public String toString() {
		return new StringBuilder()
			.append("ActivityEntity [id=").append(id)
			.append(", municipalityId=").append(municipalityId)
			.append(", activityId=").append(activityId)
			.append(", producer=").append(producer)
			.append(", customerIdentifier=").append(PiiMasker.maskPersonalNumber(customerIdentifier))
			.append(", customerType=").append(customerType)
			.append(", customerSuffix=").append(customerSuffix)
			.append(", errandIdentifier=").append(errandIdentifier)
			.append(", errandType=").append(errandType)
			.append(", title=").append(title)
			.append(", description=").append(description)
			.append(", language=").append(language)
			.append(", occurredAt=").append(occurredAt)
			.append(", activityType=").append(activityType)
			.append(", customerActionRequired=").append(customerActionRequired)
			.append(", errandCompleted=").append(errandCompleted)
			.append(", version=").append(version)
			.append(", extendedInformation=").append(extendedInformation)
			.append(", referenceData=").append(referenceData)
			.append(", created=").append(created)
			.append(", modified=").append(modified)
			.append(", tags=").append(tags)
			.append("]").toString();
	}
}
