package se.sundsvall.activityfeed.configuration;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * API keys. The protection is fail-closed: without at least one valid key the service does not start, and there is no
 * way to switch the check off. The keys are given as comma-separated lists (the environment variables API_KEYS_QUERY
 * and API_KEYS_ADMIN), so a new key can be added before the old one is removed during rotation.
 *
 * @param headerName header the key is sent in
 * @param queryKeys  keys that may call /{municipalityId}/kundhandelseFragaSynkron (the forwarding service)
 * @param adminKeys  keys that may call everything, including ingest and delete in the activity cache (business systems,
 *                   operations)
 */
@ConfigurationProperties(prefix = "security.api-key")
public record ApiKeyProperties(
	@DefaultValue(ApiKeyProperties.DEFAULT_HEADER_NAME) String headerName,
	@DefaultValue List<String> queryKeys,
	@DefaultValue List<String> adminKeys) {

	public static final String DEFAULT_HEADER_NAME = "X-API-Key";

	/** Minimum key length. openssl rand -hex 32 gives 64 characters. */
	public static final int MIN_LENGTH = 32;

	private static final Pattern VALID_KEY = Pattern.compile("^[\\x21-\\x7E]{" + MIN_LENGTH + ",}$");

	public ApiKeyProperties {
		if (headerName == null || headerName.isBlank()) {
			throw new IllegalStateException("security.api-key.header-name must not be blank");
		}
		queryKeys = normalize(queryKeys, "API_KEYS_QUERY");
		adminKeys = normalize(adminKeys, "API_KEYS_ADMIN");
		if (queryKeys.isEmpty() && adminKeys.isEmpty()) {
			throw new IllegalStateException("No API key is configured. Set API_KEYS_ADMIN and/or API_KEYS_QUERY "
				+ "(comma-separated, at least " + MIN_LENGTH + " characters per key, e.g. openssl rand -hex 32)");
		}
	}

	private static List<String> normalize(final List<String> keys, final String environmentVariable) {
		final var result = Optional.ofNullable(keys).orElse(List.of()).stream()
			.filter(key -> key != null && !key.isBlank())
			.map(String::strip)
			.distinct()
			.toList();
		// The key's value must never end up in an error message or a log, only its position.
		for (var i = 0; i < result.size(); i++) {
			if (!VALID_KEY.matcher(result.get(i)).matches()) {
				throw new IllegalStateException("Key number " + (i + 1) + " in " + environmentVariable + " is invalid: it must be at least "
					+ MIN_LENGTH + " characters, without spaces, and contain only printable ASCII characters");
			}
		}
		return result;
	}
}
