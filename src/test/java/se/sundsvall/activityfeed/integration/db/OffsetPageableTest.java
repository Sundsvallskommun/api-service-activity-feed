package se.sundsvall.activityfeed.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OffsetPageableTest {

	@Test
	void arbitraryOffset() {
		final var pageable = new OffsetPageable(7, 5);

		assertThat(pageable.getOffset()).isEqualTo(7);
		assertThat(pageable.getPageSize()).isEqualTo(5);
		assertThat(pageable.getPageNumber()).isEqualTo(1);
		assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
		assertThat(pageable.hasPrevious()).isTrue();
		assertThat(pageable.next()).isEqualTo(new OffsetPageable(12, 5));
		assertThat(pageable.previousOrFirst()).isEqualTo(new OffsetPageable(2, 5));
		assertThat(pageable.first()).isEqualTo(new OffsetPageable(0, 5));
		assertThat(pageable.withPage(3)).isEqualTo(new OffsetPageable(15, 5));
	}

	@Test
	void firstPage() {
		final var pageable = new OffsetPageable(0, 10);

		assertThat(pageable.hasPrevious()).isFalse();
		assertThat(pageable.previousOrFirst()).isEqualTo(pageable);
	}

	@Test
	void invalidValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> new OffsetPageable(-1, 10));
		assertThatIllegalArgumentException().isThrownBy(() -> new OffsetPageable(0, 0));
	}
}
