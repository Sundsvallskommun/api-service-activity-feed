package se.sundsvall.activityfeed.api.validation;

/**
 * Format of activity types: PRODUCER.PRODUCERERRANDTYPE.ACTIVITYTYPE in uppercase letters, digits and underscores,
 * without å, ä, ö (the Mina ärenden standard).
 */
public final class ActivityTypeFormat {

	/**
	 * For queries: the same pattern as the JSON schema in Skatteverket's API definition
	 * ({@code ^[A-Z]+\.(?:[A-Z0-9_.]+)+$}), plus a lone producer prefix (e.g. {@code REFKOM}) as in the schema's example.
	 * The producer must not reject queries that the forwarding service lets through.
	 */
	public static final String QUERY_PATTERN = "^[A-Z]+(?:\\.[A-Z0-9_.]+)?$";

	/**
	 * For ingest into the cache: strict, at least three parts (producer, producer errand type, activity type) and no empty
	 * parts. The producer's own data must follow the standard's naming in full.
	 */
	public static final String INGEST_PATTERN = "^[A-Z]+(?:\\.[A-Z0-9_]+){2,}$";

	/** Maximum length, matches the column activity_type. */
	public static final int MAX_LENGTH = 200;

	private ActivityTypeFormat() {}
}
