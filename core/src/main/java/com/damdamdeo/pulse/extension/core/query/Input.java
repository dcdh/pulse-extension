package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface Input {

    default Pagination pagination() {
        throw new UnsupportedOperationException();
    }
}
