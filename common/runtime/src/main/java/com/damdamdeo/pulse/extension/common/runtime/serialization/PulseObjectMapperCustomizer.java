package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.arc.Unremovable;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Singleton
@Unremovable
public final class PulseObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(final ObjectMapper objectMapper) {
        customizeObjectMapper(objectMapper);
    }

    public static void customizeObjectMapper(final ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        objectMapper.setVisibility(
                objectMapper.getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(ANY)
                        .withGetterVisibility(NONE)
                        .withSetterVisibility(NONE)
                        .withIsGetterVisibility(NONE));
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        final SimpleModule sequenceNumberModule = new SimpleModule();
        sequenceNumberModule.addSerializer(SequenceNumber.class, new SequenceNumberSerializer());
        sequenceNumberModule.addDeserializer(SequenceNumber.class, new SequenceNumberDeserializer());
        objectMapper.registerModule(sequenceNumberModule);
    }

}
