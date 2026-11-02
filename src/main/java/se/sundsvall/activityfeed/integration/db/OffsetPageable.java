package se.sundsvall.activityfeed.integration.db;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pageable with a free offset (not a page number), so that fraga.behandling.paginering (offset, limit) can be passed
 * straight to the database query. Sorting is set in the specification, not here.
 *
 * @param offset number of rows to skip
 * @param limit  maximum number of rows to fetch
 */
public record OffsetPageable(long offset, int limit)
	implements
	Pageable {

	public OffsetPageable {
		if (offset < 0) {
			throw new IllegalArgumentException("offset must not be negative");
		}
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be at least 1");
		}
	}

	@Override
	public int getPageNumber() {
		return (int) (offset / limit);
	}

	@Override
	public int getPageSize() {
		return limit;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public Sort getSort() {
		return Sort.unsorted();
	}

	@Override
	public Pageable next() {
		return new OffsetPageable(offset + limit, limit);
	}

	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? new OffsetPageable(Math.max(0, offset - limit), limit) : first();
	}

	@Override
	public Pageable first() {
		return new OffsetPageable(0, limit);
	}

	@Override
	public Pageable withPage(final int pageNumber) {
		return new OffsetPageable((long) pageNumber * limit, limit);
	}

	@Override
	public boolean hasPrevious() {
		return offset > 0;
	}
}
