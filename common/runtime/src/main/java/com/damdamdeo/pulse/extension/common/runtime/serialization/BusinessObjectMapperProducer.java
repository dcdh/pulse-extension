package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

// In common because used by writer AND query test modules
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
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        final SimpleModule businessMapperModule = new SimpleModule();
        businessMapperModule.addSerializer(SequenceNumber.class, new SequenceNumberSerializer());
        businessMapperModule.addDeserializer(SequenceNumber.class, new SequenceNumberDeserializer());
        businessMapperModule.addSerializer(OwnedBy.class, new OwnedBySerializer());
        businessMapperModule.addDeserializer(OwnedBy.class, new OwnedByDeserializer());
        businessMapperModule.addSerializer(BelongsTo.class, new BelongsToSerializer());
        businessMapperModule.addDeserializer(BelongsTo.class, new BelongsToDeserializer());
        objectMapper.registerModule(businessMapperModule);
        objectMapper.setAnnotationIntrospector(
                AnnotationIntrospector.pair(
                        new AggregateRootAnnotationIntrospector(),
                        objectMapper.getSerializationConfig().getAnnotationIntrospector()));
        return objectMapper;
    }

}
