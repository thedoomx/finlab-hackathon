package org.hackathon.finlabvalidator.persistence.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record PaginatedResult<T>(
        Collection<T> items,
        long totalCount,
        int pageSize,
        int currentPage,
        long totalPages
) {
    public PaginatedResult(List<T> items, long totalCount, int pageSize, int currentPage) {
        this(
                Collections.unmodifiableList(items),
                totalCount,
                pageSize,
                currentPage,
                (long) Math.ceil((double) totalCount / pageSize)
        );
    }

    public boolean hasNext() {
        return currentPage < totalPages;
    }

    public boolean hasPrevious() {
        return currentPage > 1;
    }
}
