package com.damdamdeo.pulse.extension.publisher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public record JsonNodeAggregateRootValue(@JsonProperty("last_version") Long lastVersion,
                                         @JsonProperty("aggregate_root_payload") byte[] payload,
                                         @JsonProperty("owned_by") String ownedBy,
                                         @JsonProperty("belongs_to") String belongsTo) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeAggregateRootValue {
        Objects.requireNonNull(lastVersion);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JsonNodeAggregateRootValue that = (JsonNodeAggregateRootValue) o;
        return Objects.equals(lastVersion, that.lastVersion)
                && Arrays.equals(payload, that.payload)
                && Objects.equals(ownedBy, that.ownedBy)
                && Objects.equals(belongsTo, that.belongsTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastVersion, Arrays.hashCode(payload), ownedBy, belongsTo);
    }

    @Override
    public String toString() {
        return "JsonNodeAggregateRootValue{" +
                "lastVersion=" + lastVersion +
                ", payload=" + Arrays.toString(payload) +
                ", ownedBy='" + ownedBy + '\'' +
                ", belongsTo='" + belongsTo + '\'' +
                '}';
    }
}
