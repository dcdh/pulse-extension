package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

public record Pagination(int page, int size) {

    public Pagination {
        Validate.isTrue(page >= 0, "page must be greater than or equal to 0");
        Validate.isTrue(size > 0, "size must be greater than 0");
    }

    public int offset() {
        return page * size;
    }
}
