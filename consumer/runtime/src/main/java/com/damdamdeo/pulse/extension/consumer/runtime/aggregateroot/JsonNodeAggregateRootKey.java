package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AggregateRootKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record JsonNodeAggregateRootKey(@JsonProperty("aggregate_root_type") String aggregateRootType,
                                       @JsonProperty("aggregate_root_id") String aggregateRootId,
                                       @JsonProperty("version") Integer version) implements AggregateRootKey {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeAggregateRootKey {
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateRootId);
        Objects.requireNonNull(version);
    }

    @Override
    public AggregateRootType toAggregateRootType() {
        return new AggregateRootType(aggregateRootType);
    }

    @Override
    public AggregateId toAggregateId() {
        return new AnyAggregateId(aggregateRootId);
    }

    @Override
    public CurrentVersionInConsumption toCurrentVersionInConsumption() {
        return new CurrentVersionInConsumption(version);
    }
}