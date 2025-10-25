package com.damdamdeo.pulse.extension.runtime.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.EventRecord;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@RegisterForReflection
public record JsonNodeEventRecord(@JsonProperty("aggregate_root_id") String aggregateRootId,
                                  @JsonProperty("aggregate_root_type") String aggregateRootType,
                                  @JsonProperty("version") Integer version,
                                  @JsonProperty("creation_date") Long createDate,
                                  @JsonProperty("event_type") String eventType,
                                  @JsonProperty("event_payload") byte[] eventPayload,
                                  @JsonProperty("owned_by") String ownedBy) implements EventRecord {

    public JsonNodeEventRecord {
        Objects.requireNonNull(aggregateRootId);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(version);
        Objects.requireNonNull(createDate);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(eventPayload);
        Objects.requireNonNull(ownedBy);
    }

    @Override
    public AggregateId toAggregateId() {
        return new AnyAggregateId(aggregateRootId);
    }

    @Override
    public AggregateRootType toAggregateRootType() {
        return new AggregateRootType(aggregateRootType);
    }

    @Override
    public CurrentVersionInConsumption toCurrentVersionInConsumption() {
        return new CurrentVersionInConsumption(version);
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
                "aggregateRootId='" + aggregateRootId + '\'' +
                ", aggregateRootType='" + aggregateRootType + '\'' +
                ", version=" + version +
                ", createDate=" + createDate +
                ", eventType='" + eventType + '\'' +
                ", eventPayload=" + Arrays.toString(eventPayload) +
                ", ownedBy='" + ownedBy + '\'' +
                '}';
    }
}
