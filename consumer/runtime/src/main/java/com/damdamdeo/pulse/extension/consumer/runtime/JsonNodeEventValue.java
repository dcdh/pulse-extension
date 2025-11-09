package com.damdamdeo.pulse.extension.consumer.runtime;

import com.damdamdeo.pulse.extension.core.consumer.EventValue;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@RegisterForReflection
public record JsonNodeEventValue(@JsonProperty("creation_date") Long createDate,
                                 @JsonProperty("event_type") String eventType,
                                 @JsonProperty("event_payload") byte[] eventPayload,
                                 @JsonProperty("owned_by") String ownedBy) implements EventValue {

    public JsonNodeEventValue {
        Objects.requireNonNull(createDate);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(eventPayload);
        Objects.requireNonNull(ownedBy);
    }

    @Override
    public Instant toCreationDate() {
        return Instant.ofEpochMilli(createDate);
    }

    @Override
    public EventType toEventType() {
        return new EventType(eventType);
    }

    @Override
    public EncryptedPayload toEncryptedEventPayload() {
        return new EncryptedPayload(eventPayload);
    }

    @Override
    public OwnedBy toOwnedBy() {
        return new OwnedBy(ownedBy);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JsonNodeEventValue that = (JsonNodeEventValue) o;
        return Objects.equals(ownedBy, that.ownedBy)
                && Objects.equals(createDate, that.createDate)
                && Objects.equals(eventType, that.eventType)
                && Arrays.equals(eventPayload, that.eventPayload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createDate, eventType, Arrays.hashCode(eventPayload), ownedBy);
    }

    @Override
    public String toString() {
        return "JsonNodeEventRecord{" +
                "createDate=" + createDate +
                ", eventType='" + eventType + '\'' +
                ", eventPayload=" + Arrays.toString(eventPayload) +
                ", ownedBy='" + ownedBy + '\'' +
                '}';
    }
}
