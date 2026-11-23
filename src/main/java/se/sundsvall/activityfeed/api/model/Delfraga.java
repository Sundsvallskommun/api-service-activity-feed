package se.sundsvall.activityfeed.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Utfall per producent och part")
public class Delfraga {

	private String producent;

	private Part part;

	@Schema(examples = "OK")
	private String status;

	private String message;

	@Schema(examples = "200")
	private Integer httpCode;

	public static Delfraga create() {
		return new Delfraga();
	}

	public String getProducent() {
		return producent;
	}

	public void setProducent(final String producent) {
		this.producent = producent;
	}

	public Delfraga withProducent(final String producent) {
		this.producent = producent;
		return this;
	}

	public Part getPart() {
		return part;
	}

	public void setPart(final Part part) {
		this.part = part;
	}

	public Delfraga withPart(final Part part) {
		this.part = part;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Delfraga withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public Delfraga withMessage(final String message) {
		this.message = message;
		return this;
	}

	public Integer getHttpCode() {
		return httpCode;
	}

	public void setHttpCode(final Integer httpCode) {
		this.httpCode = httpCode;
	}

	public Delfraga withHttpCode(final Integer httpCode) {
		this.httpCode = httpCode;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(producent, part, status, message, httpCode);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Delfraga other)) {
			return false;
		}
		return Objects.equals(producent, other.producent) && Objects.equals(part, other.part) && Objects.equals(status, other.status) && Objects.equals(message, other.message) && Objects.equals(httpCode, other.httpCode);
	}

	@Override
	public String toString() {
		return "Delfraga [producent=" + producent +
			", part=" + part +
			", status=" + status +
			", message=" + message +
			", httpCode=" + httpCode +
			"]";
	}
}
