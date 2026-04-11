package com.damdamdeo.pulse.extension.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public record JsonNodeEventValue(@JsonProperty("stored_at") Long storedAt,
                                 @JsonProperty("event_type") String eventType,
                                 @JsonProperty("event_payload") byte[] eventPayload,
                                 @JsonProperty("owned_by") String ownedBy,
                                 @JsonProperty("belongs_to") String belongsTo,
                                 @JsonProperty("executed_by") String executedBy) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeEventValue {
        Objects.requireNonNull(storedAt);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(eventPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
        Objects.requireNonNull(executedBy);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JsonNodeEventValue that = (JsonNodeEventValue) o;
        return Objects.equals(ownedBy, that.ownedBy)
                && Objects.equals(storedAt, that.storedAt)
                && Objects.equals(eventType, that.eventType)
                && Arrays.equals(eventPayload, that.eventPayload)
                && Objects.equals(belongsTo, that.belongsTo)
                && Objects.equals(executedBy, that.executedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storedAt, eventType, Arrays.hashCode(eventPayload), ownedBy, belongsTo, executedBy);
    }

    @Override
    public String toString() {
        return "JsonNodeEventRecord{" +
                "storedAt=" + storedAt +
                ", eventType='" + eventType + '\'' +
                ", eventPayload=" + Arrays.toString(eventPayload) +
                ", ownedBy='" + ownedBy + '\'' +
                ", belongsTo='" + belongsTo + '\'' +
                ", executedBy='" + executedBy + '\'' +
                '}';
    }
}
