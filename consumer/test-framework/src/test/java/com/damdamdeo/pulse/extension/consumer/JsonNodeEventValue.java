package com.damdamdeo.pulse.extension.consumer;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.StoredAt;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public record JsonNodeEventValue(@JsonProperty("stored_at") StoredAt storedAt,
                                 @JsonProperty("event_type") EventType eventType,
                                 @JsonProperty("event_payload") byte[] eventPayload,
                                 @JsonProperty("owned_by") OwnedBy ownedBy,
                                 @JsonProperty("belongs_to") BelongsTo belongsTo,
                                 @JsonProperty("executed_by") ExecutedByEncoded executedByEncoded) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeEventValue {
        Objects.requireNonNull(storedAt);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(eventPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
        Objects.requireNonNull(executedByEncoded);
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
                && Objects.equals(executedByEncoded, that.executedByEncoded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storedAt, eventType, Arrays.hashCode(eventPayload), ownedBy, belongsTo, executedByEncoded);
    }

    @Override
    public String toString() {
        return "JsonNodeEventRecord{" +
                "storedAt=" + storedAt +
                ", eventType='" + eventType + '\'' +
                ", eventPayload=" + Arrays.toString(eventPayload) +
                ", ownedBy='" + ownedBy + '\'' +
                ", belongsTo='" + belongsTo + '\'' +
                ", executedByEncoded='" + executedByEncoded + '\'' +
                '}';
    }
}
