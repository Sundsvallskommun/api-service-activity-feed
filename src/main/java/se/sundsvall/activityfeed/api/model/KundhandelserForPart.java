package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Kundhändelser för en part")
public class KundhandelserForPart {

	private Part part;

	@Schema(description = "Antal kundhändelser som matchar frågan, före paginering")
	private long totaltAntalKundhandelser;

	private List<Kundhandelse> kundhandelserForPart;

	public static KundhandelserForPart create() {
		return new KundhandelserForPart();
	}

	public Part getPart() {
		return part;
	}

	public void setPart(final Part part) {
		this.part = part;
	}

	public KundhandelserForPart withPart(final Part part) {
		this.part = part;
		return this;
	}

	public long getTotaltAntalKundhandelser() {
		return totaltAntalKundhandelser;
	}

	public void setTotaltAntalKundhandelser(final long totaltAntalKundhandelser) {
		this.totaltAntalKundhandelser = totaltAntalKundhandelser;
	}

	public KundhandelserForPart withTotaltAntalKundhandelser(final long totaltAntalKundhandelser) {
		this.totaltAntalKundhandelser = totaltAntalKundhandelser;
		return this;
	}

	public List<Kundhandelse> getKundhandelserForPart() {
		return kundhandelserForPart;
	}

	public void setKundhandelserForPart(final List<Kundhandelse> kundhandelserForPart) {
		this.kundhandelserForPart = kundhandelserForPart;
	}

	public KundhandelserForPart withKundhandelserForPart(final List<Kundhandelse> kundhandelserForPart) {
		this.kundhandelserForPart = kundhandelserForPart;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(part, totaltAntalKundhandelser, kundhandelserForPart);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final KundhandelserForPart other)) {
			return false;
		}
		return Objects.equals(part, other.part) && (totaltAntalKundhandelser == other.totaltAntalKundhandelser) && Objects.equals(kundhandelserForPart, other.kundhandelserForPart);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("KundhandelserForPart [part=").append(part)
			.append(", totaltAntalKundhandelser=").append(totaltAntalKundhandelser)
			.append(", kundhandelserForPart=").append(kundhandelserForPart)
			.append("]").toString();
	}
}
