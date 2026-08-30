package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;

public record Page<T>(List<T> content, Pagination pagination, long totalElements) {

    public Page {
        Objects.requireNonNull(content);
        Objects.requireNonNull(pagination);
        Validate.isTrue(totalElements >= 0, "totalElements must be greater than or equal to 0");
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / pagination.size());
    }

    public boolean hasNext() {
        return pagination.page() + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return pagination.page() > 0;
    }
}
