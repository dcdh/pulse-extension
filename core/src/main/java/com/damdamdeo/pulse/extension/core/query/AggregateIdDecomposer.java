package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AggregateIdDecomposer {

    public Set<AggregateId> unCompound(final Set<AggregateId> aggregateIds) {
        return aggregateIds.stream()
                .flatMap(aggregateId -> {
                    final String[] split = aggregateId.id().split(AggregateId.SEPARATOR);
                    final List<AggregateId> uncompounded = new ArrayList<>(split.length);
                    uncompounded.add(new AnyAggregateId(aggregateId.id()));
                    for (int i = 1; i < split.length; i++) {
                        uncompounded.add(new AnyAggregateId(String.join(AggregateId.SEPARATOR,
                                Arrays.asList(split).subList(0, i))));
                    }
                    return uncompounded.stream();
                })
                .collect(Collectors.toSet());
    }
}
