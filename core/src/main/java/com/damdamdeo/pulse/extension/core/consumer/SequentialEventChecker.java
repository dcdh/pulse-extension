package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.LastConsumedAggregateVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SequentialEventChecker {

    void check(final LastConsumedAggregateVersion lastConsumedAggregateVersion,
               final CurrentVersionInConsumption currentVersionInConsumption) throws SequenceNotRespectedException {
        Objects.requireNonNull(lastConsumedAggregateVersion);
        Objects.requireNonNull(currentVersionInConsumption);
        if (lastConsumedAggregateVersion.version() < currentVersionInConsumption.version() - 1) {
            final List<AggregateVersion> missingAggregateVersions = new ArrayList<>(currentVersionInConsumption.version() - 1 - lastConsumedAggregateVersion.version());
            for (int missingAggregateVersion = lastConsumedAggregateVersion.version() + 1; missingAggregateVersion <= currentVersionInConsumption.version() - 1; missingAggregateVersion++) {
                missingAggregateVersions.add(new AggregateVersion(missingAggregateVersion));
            }
            throw new SequenceNotRespectedException(missingAggregateVersions);
        }
    }
}
