package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Paginering. Utan limit lämnas alla kundhändelser från offset till slutet.")
public class Paginering {

	@Schema(examples = "0", requiredMode = REQUIRED)
	@NotNull
	@Min(0)
	private Integer offset;

	@Schema(examples = "20")
	@Min(1)
	@Max(value = 1000, message = "paginering.limit får vara högst {value}")
	private Integer limit;

	public static Paginering create() {
		return new Paginering();
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(final Integer offset) {
		this.offset = offset;
	}

	public Paginering withOffset(final Integer offset) {
		this.offset = offset;
		return this;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(final Integer limit) {
		this.limit = limit;
	}

	public Paginering withLimit(final Integer limit) {
		this.limit = limit;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(offset, limit);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Paginering other)) {
			return false;
		}
		return Objects.equals(offset, other.offset) && Objects.equals(limit, other.limit);
	}

	@Override
	public String toString() {
		return "Paginering [offset=" + offset +
			", limit=" + limit +
			"]";
	}
}
