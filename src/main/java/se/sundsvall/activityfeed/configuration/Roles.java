package se.sundsvall.activityfeed.configuration;

/**
 * Roles an API key grants.
 */
public final class Roles {

	/** May run queries (the forwarding service). */
	public static final String QUERY = "QUERY";

	/** May do everything, including maintaining the activity cache and reading operational information. */
	public static final String ADMIN = "ADMIN";

	private Roles() {}
}
