package com.damdamdeo.pulse.extension.core.consumer.checker;

import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.List;
import java.util.Objects;

public final class SequenceNotRespectedException extends RuntimeException {

    private final List<AggregateVersion> missingAggregateVersions;

    public SequenceNotRespectedException(final List<AggregateVersion> missingAggregateVersions) {
        this.missingAggregateVersions = Objects.requireNonNull(missingAggregateVersions);
    }

    public List<AggregateVersion> missingAggregateVersion() {
        return missingAggregateVersions;
    }
}
