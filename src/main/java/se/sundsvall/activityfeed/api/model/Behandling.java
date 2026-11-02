package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;

@Schema(description = "Styr sortering och paginering av kundhändelserna i svaret. Paginering kräver sortering.")
public class Behandling {

	private List<@Valid Sortering> sortering;

	@Valid
	private Paginering paginering;

	public static Behandling create() {
		return new Behandling();
	}

	public List<Sortering> getSortering() {
		return sortering;
	}

	public void setSortering(final List<Sortering> sortering) {
		this.sortering = sortering;
	}

	public Behandling withSortering(final List<Sortering> sortering) {
		this.sortering = sortering;
		return this;
	}

	public Paginering getPaginering() {
		return paginering;
	}

	public void setPaginering(final Paginering paginering) {
		this.paginering = paginering;
	}

	public Behandling withPaginering(final Paginering paginering) {
		this.paginering = paginering;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sortering, paginering);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Behandling other)) {
			return false;
		}
		return Objects.equals(sortering, other.sortering) && Objects.equals(paginering, other.paginering);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("Behandling [sortering=").append(sortering)
			.append(", paginering=").append(paginering)
			.append("]").toString();
	}
}
