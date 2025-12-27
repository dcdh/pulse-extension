package com.damdamdeo.pulse.extension.consumer.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.EventKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record JsonNodeEventKey(@JsonProperty("aggregate_root_type") String aggregateRootType,
                               @JsonProperty("aggregate_root_id") String aggregateRootId,
                               @JsonProperty("version") Integer version) implements EventKey {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeEventKey {
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

    @Override
    public String toString() {
        return "JsonNodeEventKey{" +
                "aggregateRootType='" + aggregateRootType + '\'' +
                ", aggregateRootId='" + aggregateRootId + '\'' +
                ", version=" + version +
                '}';
    }
}
