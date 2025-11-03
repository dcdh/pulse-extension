package com.damdamdeo.pulse.extension.runtime.consumer;

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
    public String toString() {
        return "JsonNodeEventRecord{" +
                "createDate=" + createDate +
                ", eventType='" + eventType + '\'' +
                ", eventPayload=" + Arrays.toString(eventPayload) +
                ", ownedBy='" + ownedBy + '\'' +
                '}';
    }
}
