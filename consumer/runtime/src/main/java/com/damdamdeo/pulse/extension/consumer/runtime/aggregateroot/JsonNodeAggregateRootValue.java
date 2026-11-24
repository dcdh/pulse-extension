package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AggregateRootValue;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonNodeAggregateRootValue(
        @JsonProperty("aggregate_root_payload")
        Encrypted<byte[]> encryptedPayload,

        @JsonProperty("owned_by")
        OwnedBy ownedBy,

        @JsonProperty("belongs_to")
        BelongsTo belongsTo) implements AggregateRootValue {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public JsonNodeAggregateRootValue {
        Objects.requireNonNull(encryptedPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
    }

}
