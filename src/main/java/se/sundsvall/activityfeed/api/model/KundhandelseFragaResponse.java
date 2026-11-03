package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Response to a query, same schema as in Skatteverket's API definition.
 */
@Schema(name = "KundhandelseFragaResponse", description = "Svar med kundhändelser per part")
public class KundhandelseFragaResponse {

	private List<KundhandelserForPart> kundhandelser;

	private Metadata metadata;

	private List<Delfraga> delfragor;

	public static KundhandelseFragaResponse create() {
		return new KundhandelseFragaResponse();
	}

	public List<KundhandelserForPart> getKundhandelser() {
		return kundhandelser;
	}

	public void setKundhandelser(final List<KundhandelserForPart> kundhandelser) {
		this.kundhandelser = kundhandelser;
	}

	public KundhandelseFragaResponse withKundhandelser(final List<KundhandelserForPart> kundhandelser) {
		this.kundhandelser = kundhandelser;
		return this;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(final Metadata metadata) {
		this.metadata = metadata;
	}

	public KundhandelseFragaResponse withMetadata(final Metadata metadata) {
		this.metadata = metadata;
		return this;
	}

	public List<Delfraga> getDelfragor() {
		return delfragor;
	}

	public void setDelfragor(final List<Delfraga> delfragor) {
		this.delfragor = delfragor;
	}

	public KundhandelseFragaResponse withDelfragor(final List<Delfraga> delfragor) {
		this.delfragor = delfragor;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(kundhandelser, metadata, delfragor);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final KundhandelseFragaResponse other)) {
			return false;
		}
		return Objects.equals(kundhandelser, other.kundhandelser) && Objects.equals(metadata, other.metadata) && Objects.equals(delfragor, other.delfragor);
	}

	@Override
	public String toString() {
		return "KundhandelseFragaResponse [kundhandelser=" + kundhandelser +
			", metadata=" + metadata +
			", delfragor=" + delfragor +
			"]";
	}
}
