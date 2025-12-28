package com.damdamdeo.pulse.extension.consumer.runtime.event;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeEventValueDeserializer extends ObjectMapperDeserializer<JsonNodeEventValue> {

    public JsonNodeEventValueDeserializer() {
        super(JsonNodeEventValue.class);
    }
}
