package se.sundsvall.activityfeed.api.validation;

/**
 * Hard limits for incoming requests. The standard sets no maximums. These limit the resources one request can use and
 * sit far above realistic volumes (a query from the forwarding service normally covers one or two parties). Raise them
 * if needed.
 */
public final class Limits {

	/** Parties per query. Each party becomes its own database query. */
	public static final int MAX_PARTIES = 100;

	/** Activity types in a filter. */
	public static final int MAX_ACTIVITY_TYPES = 100;

	/** Tags in a filter or on an activity. */
	public static final int MAX_TAGS = 100;

	/** Length of a tag, matches the column activity_tag.tag. */
	public static final int MAX_TAG_LENGTH = 100;

	/** Largest allowed paginering.limit. */
	public static final int MAX_LIMIT = 1000;

	/** Activities per batch sent to the cache (POST /{municipalityId}/kundhandelser). */
	public static final int MAX_BATCH_SIZE = 1000;

	/** Length of skv_client_correlation_id. */
	public static final int MAX_CORRELATION_ID_LENGTH = 200;

	/**
	 * skv_client_correlation_id: printable ASCII characters (spaces included), at least one visible character and a
	 * bounded length. The API definition allows any format. The service description recommends a UUID.
	 */
	public static final String CORRELATION_ID_PATTERN = "^(?=.*[\\x21-\\x7E])[\\x20-\\x7E]{1," + MAX_CORRELATION_ID_LENGTH + "}$";

	private Limits() {}
}
