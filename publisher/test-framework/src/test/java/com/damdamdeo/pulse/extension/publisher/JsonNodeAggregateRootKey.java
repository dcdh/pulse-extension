package com.damdamdeo.pulse.extension.publisher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record JsonNodeAggregateRootKey(@JsonProperty("aggregate_root_type") String aggregateRootType,
                                       @JsonProperty("aggregate_root_id") String aggregateRootId) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeAggregateRootKey {
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateRootId);
    }

    @Override
    public String toString() {
        return "JsonNodeEventKey{" +
                "aggregateRootType='" + aggregateRootType + '\'' +
                ", aggregateRootId='" + aggregateRootId + '\'' +
                '}';
    }
}
