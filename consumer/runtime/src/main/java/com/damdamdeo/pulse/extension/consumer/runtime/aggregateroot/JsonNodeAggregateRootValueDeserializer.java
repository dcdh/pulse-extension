package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeAggregateRootValueDeserializer extends ObjectMapperDeserializer<JsonNodeAggregateRootValue> {

    public JsonNodeAggregateRootValueDeserializer() {
        super(JsonNodeAggregateRootValue.class);
    }
}
