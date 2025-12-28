package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AggregateRootValue;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public record JsonNodeAggregateRootValue(@JsonProperty("aggregate_root_payload") byte[] payload,
                                         @JsonProperty("owned_by") String ownedBy,
                                         @JsonProperty("belongs_to") String belongsTo) implements AggregateRootValue {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeAggregateRootValue {
        Objects.requireNonNull(payload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
    }

    @Override
    public EncryptedPayload toEncryptedPayload() {
        return new EncryptedPayload(payload);
    }

    @Override
    public OwnedBy toOwnedBy() {
        return new OwnedBy(ownedBy);
    }

    @Override
    public BelongsTo toBelongsTo() {
        return new BelongsTo(new AnyAggregateId(belongsTo));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JsonNodeAggregateRootValue that = (JsonNodeAggregateRootValue) o;
        return Objects.equals(ownedBy, that.ownedBy)
                && Arrays.equals(payload, that.payload)
                && Objects.equals(belongsTo, that.belongsTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(payload), ownedBy, belongsTo);
    }

    @Override
    public String toString() {
        return "JsonNodeAggregateRootValue{" +
                "payload=" + Arrays.toString(payload) +
                ", ownedBy='" + ownedBy + '\'' +
                ", belongsTo='" + belongsTo + '\'' +
                '}';
    }
}
