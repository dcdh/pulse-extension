package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AggregateIdDecomposer {

    public <K extends AggregateId> Set<AggregateId> unCompound(final K aggregateId) {
        Objects.requireNonNull(aggregateId);
        final String[] split = aggregateId.id().split(AggregateId.SEPARATOR);
        final Set<AggregateId> uncompounded = new HashSet<>(split.length);
        uncompounded.add(AnyAggregateId.from(aggregateId));
        for (int i = 1; i < split.length; i++) {
            uncompounded.add(new AnyAggregateId(String.join(AggregateId.SEPARATOR,
                    Arrays.asList(split).subList(0, i))));
        }
        return uncompounded;
    }
}
