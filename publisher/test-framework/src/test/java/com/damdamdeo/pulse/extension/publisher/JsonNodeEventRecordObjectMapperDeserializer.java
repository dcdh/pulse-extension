package com.damdamdeo.pulse.extension.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class JsonNodeEventRecordObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventValue> {

    public JsonNodeEventRecordObjectMapperDeserializer(final ObjectMapper objectMapper) {
        super(JsonNodeEventValue.class, objectMapper);
    }
}
