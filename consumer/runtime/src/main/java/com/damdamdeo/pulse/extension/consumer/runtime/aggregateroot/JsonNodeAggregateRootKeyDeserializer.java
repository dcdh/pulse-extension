package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeAggregateRootKeyDeserializer extends ObjectMapperDeserializer<JsonNodeAggregateRootKey> {

    public JsonNodeAggregateRootKeyDeserializer() {
        super(JsonNodeAggregateRootKey.class);
    }
}
