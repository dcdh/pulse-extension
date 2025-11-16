package com.damdamdeo.pulse.extension.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class JsonNodeEventKeyObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventKey> {

    public JsonNodeEventKeyObjectMapperDeserializer(final ObjectMapper objectMapper) {
        super(JsonNodeEventKey.class, objectMapper);
    }
}
