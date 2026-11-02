package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * An activity as stored in the cache: party, activity and tags.
 */
@Schema(description = "Kundhändelse i ärendecachen", accessMode = READ_ONLY)
public class CachadKundhandelse {

	private Part part;

	private Kundhandelse kundhandelse;

	private List<String> taggar;

	public static CachadKundhandelse create() {
		return new CachadKundhandelse();
	}

	public Part getPart() {
		return part;
	}

	public void setPart(final Part part) {
		this.part = part;
	}

	public CachadKundhandelse withPart(final Part part) {
		this.part = part;
		return this;
	}

	public Kundhandelse getKundhandelse() {
		return kundhandelse;
	}

	public void setKundhandelse(final Kundhandelse kundhandelse) {
		this.kundhandelse = kundhandelse;
	}

	public CachadKundhandelse withKundhandelse(final Kundhandelse kundhandelse) {
		this.kundhandelse = kundhandelse;
		return this;
	}

	public List<String> getTaggar() {
		return taggar;
	}

	public void setTaggar(final List<String> taggar) {
		this.taggar = taggar;
	}

	public CachadKundhandelse withTaggar(final List<String> taggar) {
		this.taggar = taggar;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(part, kundhandelse, taggar);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final CachadKundhandelse other)) {
			return false;
		}
		return Objects.equals(part, other.part) && Objects.equals(kundhandelse, other.kundhandelse) && Objects.equals(taggar, other.taggar);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("CachadKundhandelse [part=").append(part)
			.append(", kundhandelse=").append(kundhandelse)
			.append(", taggar=").append(taggar)
			.append("]").toString();
	}
}
