package com.damdamdeo.pulse.extension.consumer.runtime.event;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeEventKeyDeserializer extends ObjectMapperDeserializer<JsonNodeEventKey> {

    public JsonNodeEventKeyDeserializer() {
        super(JsonNodeEventKey.class);
    }
}
