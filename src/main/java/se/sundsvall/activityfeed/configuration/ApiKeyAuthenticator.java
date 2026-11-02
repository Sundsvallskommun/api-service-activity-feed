package se.sundsvall.activityfeed.configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Compares a presented API key with the configured ones. The keys are held in memory only as SHA-256 hashes, and the
 * comparison runs in constant time against every key, so the response time does not reveal how much of a key matches.
 * The caller is identified by the key's position (e.g. admin-1), never by the key.
 */
@Component
public class ApiKeyAuthenticator {

	private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticator.class);

	private final List<Key> keys;

	public ApiKeyAuthenticator(final ApiKeyProperties properties) {
		final var list = new ArrayList<Key>();
		for (var i = 0; i < properties.adminKeys().size(); i++) {
			list.add(new Key("admin-" + (i + 1), sha256(properties.adminKeys().get(i)), Set.of(Roles.ADMIN, Roles.QUERY)));
		}
		for (var i = 0; i < properties.queryKeys().size(); i++) {
			list.add(new Key("query-" + (i + 1), sha256(properties.queryKeys().get(i)), Set.of(Roles.QUERY)));
		}
		this.keys = List.copyOf(list);
		LOG.info("API key protection active with {} admin key(s) and {} query key(s)", properties.adminKeys().size(), properties.queryKeys().size());
	}

	/**
	 * @param  presented the value of the API key header
	 * @return           an authentication with the key's roles if the key is valid
	 */
	public Optional<Authentication> authenticate(final String presented) {
		if (presented == null || presented.isEmpty()) {
			return Optional.empty();
		}
		final var hash = sha256(presented);
		String id = null;
		final var roles = new LinkedHashSet<String>();
		// Compare against every key without stopping at the first match.
		for (final var key : keys) {
			if (MessageDigest.isEqual(key.hash(), hash)) {
				id = id == null ? key.id() : id;
				roles.addAll(key.roles());
			}
		}
		if (id == null) {
			return Optional.empty();
		}
		final var authorities = roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
		return Optional.of(new PreAuthenticatedAuthenticationToken(id, null, authorities));
	}

	static byte[] sha256(final String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available in the JVM", e);
		}
	}

	private record Key(String id, byte[] hash, Set<String> roles) {
	}
}
