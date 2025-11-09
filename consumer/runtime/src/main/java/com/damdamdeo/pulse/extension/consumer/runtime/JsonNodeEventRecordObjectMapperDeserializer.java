package com.damdamdeo.pulse.extension.consumer.runtime;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeEventRecordObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventValue> {

    public JsonNodeEventRecordObjectMapperDeserializer() {
        super(JsonNodeEventValue.class);
    }
}
