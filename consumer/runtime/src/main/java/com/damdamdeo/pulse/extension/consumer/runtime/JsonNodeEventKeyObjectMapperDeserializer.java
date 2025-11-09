package com.damdamdeo.pulse.extension.consumer.runtime;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeEventKeyObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventKey> {

    public JsonNodeEventKeyObjectMapperDeserializer() {
        super(JsonNodeEventKey.class);
    }
}
