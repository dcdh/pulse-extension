package com.damdamdeo.pulse.extension.runtime.consumer;

import com.damdamdeo.pulse.extension.core.consumer.EventKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

@RegisterForReflection
public record JsonNodeEventKey(@JsonProperty("aggregate_root_id") String aggregateRootId,
                               @JsonProperty("aggregate_root_type") String aggregateRootType,
                               @JsonProperty("version") Integer version) implements EventKey {

    public JsonNodeEventKey {
        Objects.requireNonNull(aggregateRootId);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(version);
    }

    @Override
    public String toString() {
        return "JsonNodeEventKey{" +
                "aggregateRootId='" + aggregateRootId + '\'' +
                ", aggregateRootType='" + aggregateRootType + '\'' +
                ", version=" + version +
                '}';
    }
}
