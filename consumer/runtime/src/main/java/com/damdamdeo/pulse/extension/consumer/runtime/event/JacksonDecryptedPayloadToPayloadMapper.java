package com.damdamdeo.pulse.extension.consumer.runtime.event;

import com.damdamdeo.pulse.extension.core.consumer.DecryptedPayloadToPayloadMapper;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Objects;

@Singleton
@Unremovable
@DefaultBean
public final class JacksonDecryptedPayloadToPayloadMapper implements DecryptedPayloadToPayloadMapper<JsonNode> {

    private final ObjectMapper objectMapper;

    public JacksonDecryptedPayloadToPayloadMapper(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public JsonNode map(final DecryptedPayload decryptedPayload) throws IOException {
        Objects.requireNonNull(decryptedPayload);
        return objectMapper.readTree(decryptedPayload.payload());
    }
}
