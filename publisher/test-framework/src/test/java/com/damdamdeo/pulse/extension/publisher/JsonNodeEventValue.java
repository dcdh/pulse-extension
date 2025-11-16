package com.damdamdeo.pulse.extension.publisher;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public record JsonNodeEventValue(@JsonProperty("creation_date") Long createDate,
                                 @JsonProperty("event_type") String eventType,
                                 @JsonProperty("event_payload") byte[] eventPayload,
                                 @JsonProperty("owned_by") String ownedBy,
                                 @JsonProperty("belongs_to") String belongsTo) {

    public JsonNodeEventValue {
        Objects.requireNonNull(createDate);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(eventPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        JsonNodeEventValue that = (JsonNodeEventValue) o;
        return Objects.equals(ownedBy, that.ownedBy)
                && Objects.equals(createDate, that.createDate)
                && Objects.equals(eventType, that.eventType)
                && Arrays.equals(eventPayload, that.eventPayload)
                && Objects.equals(belongsTo, that.belongsTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createDate, eventType, Arrays.hashCode(eventPayload), ownedBy, belongsTo);
    }

    @Override
    public String toString() {
        return "JsonNodeEventRecord{" +
                "createDate=" + createDate +
                ", eventType='" + eventType + '\'' +
                ", eventPayload=" + Arrays.toString(eventPayload) +
                ", ownedBy='" + ownedBy + '\'' +
                ", belongsTo='" + belongsTo + '\'' +
                '}';
    }
}
