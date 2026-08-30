package com.damdamdeo.pulse.extension.core.traceability;

import java.time.ZonedDateTime;
import java.util.Objects;

public record ExecutedAt(ZonedDateTime at) {

    public ExecutedAt {
        Objects.requireNonNull(at);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ExecutedAt other)) {
            return false;
        }
        return at.toInstant().equals(other.at.toInstant());
    }

    @Override
    public int hashCode() {
        return at.toInstant().hashCode();
    }
}
