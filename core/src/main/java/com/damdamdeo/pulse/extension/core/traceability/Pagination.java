package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

public record Pagination(int page, int size) {

    public Pagination {
        Validate.isTrue(page >= 0, "page must be greater than or equal to 0");
        Validate.isTrue(size >= -1, "size must be greater than or equal to -1");
    }

    public int offset() {
        return page * size;
    }

    public boolean loadAll() {
        return size == -1;
    }
}
