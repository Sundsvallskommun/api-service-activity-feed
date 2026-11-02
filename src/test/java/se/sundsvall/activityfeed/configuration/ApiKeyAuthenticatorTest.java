package se.sundsvall.activityfeed.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticatorTest {

	private static final String QUERY_1 = "f".repeat(40);
	private static final String QUERY_2 = "g".repeat(40);
	private static final String ADMIN = "a".repeat(40);

	private final ApiKeyAuthenticator authenticator = new ApiKeyAuthenticator(new ApiKeyProperties("X-API-Key", List.of(QUERY_1, QUERY_2), List.of(ADMIN)));

	@Test
	void queryKeyGrantsQueryRole() {
		assertThat(authenticator.authenticate(QUERY_2)).hasValueSatisfying(authentication -> {
			assertThat(authentication.isAuthenticated()).isTrue();
			assertThat(authentication.getName()).isEqualTo("query-2");
			assertThat(authentication.getCredentials()).isNull();
			assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_QUERY");
		});
	}

	@Test
	void adminKeyGrantsBothRoles() {
		assertThat(authenticator.authenticate(ADMIN)).hasValueSatisfying(authentication -> {
			assertThat(authentication.getName()).isEqualTo("admin-1");
			assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_QUERY");
		});
	}

	@Test
	void keyInBothListsGetsAdmin() {
		final var duplicate = new ApiKeyAuthenticator(new ApiKeyProperties("X-API-Key", List.of(ADMIN), List.of(ADMIN)));

		assertThat(duplicate.authenticate(ADMIN)).hasValueSatisfying(authentication -> assertThat(authentication.getAuthorities())
			.extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_QUERY"));
	}

	@Test
	void wrongKeyGivesNoAuthentication() {
		assertThat(authenticator.authenticate("x".repeat(40))).isEmpty();
		assertThat(authenticator.authenticate(QUERY_1.substring(1))).isEmpty();
		assertThat(authenticator.authenticate("")).isEmpty();
		assertThat(authenticator.authenticate(null)).isEmpty();
	}
}
