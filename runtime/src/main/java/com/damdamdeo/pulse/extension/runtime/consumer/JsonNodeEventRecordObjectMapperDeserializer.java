package com.damdamdeo.pulse.extension.runtime.consumer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public final class JsonNodeEventRecordObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventRecord> {

    public JsonNodeEventRecordObjectMapperDeserializer() {
        super(JsonNodeEventRecord.class);
    }
}
