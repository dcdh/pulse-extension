package com.damdamdeo.pulse.extension.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record JsonNodeEventKey(@JsonProperty("aggregate_root_type") String aggregateRootType,
                               @JsonProperty("aggregate_root_id") String aggregateRootId,
                               @JsonProperty("version") Integer version) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeEventKey {
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateRootId);
        Objects.requireNonNull(version);
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
