package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.time.ZonedDateTime;
import java.util.Objects;

public record DownloadedAt(ZonedDateTime at) {

    public DownloadedAt {
        Objects.requireNonNull(at);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DownloadedAt other)) {
            return false;
        }
        return at.toInstant().equals(other.at.toInstant());
    }

    @Override
    public int hashCode() {
        return at.toInstant().hashCode();
    }
}
