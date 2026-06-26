package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.arc.All;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Singleton
@Unremovable
public final class BusinessObjectMapperProducer {

    @Produces
    @BusinessMapper
    public ObjectMapper produceBusinessMapper(@All List<BusinessObjectMapperCustomizer> customizers) {
        final ObjectMapper objectMapper = new ObjectMapper();
        customizers.forEach(customizer -> customizer.customize(objectMapper));
        return customizeObjectMapper(objectMapper);
    }

    public static ObjectMapper customizeObjectMapper(final ObjectMapper objectMapper) {
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
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        final SimpleModule sequenceNumberModule = new SimpleModule();
        sequenceNumberModule.addSerializer(SequenceNumber.class, new SequenceNumberSerializer());
        sequenceNumberModule.addDeserializer(SequenceNumber.class, new SequenceNumberDeserializer());
        objectMapper.registerModule(sequenceNumberModule);
        return objectMapper;
    }

}
