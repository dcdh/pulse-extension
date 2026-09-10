package com.damdamdeo.pulse.extension.traceability.runtime.api.deserializer;

import com.damdamdeo.pulse.extension.common.runtime.serialization.BusinessObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.ExecutedAt;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.TraceId;
import com.fasterxml.jackson.annotation.JsonInclude;
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

@Singleton
@Unremovable
public class TraceabilityObjectMapperProducer {

    @Produces
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
        final SimpleModule traceabilityMapperModule = new SimpleModule();
        traceabilityMapperModule.addSerializer(AggregateId.class, new AggregateIdSerializer());
        traceabilityMapperModule.addSerializer(ExecutedByHashed.class, new ExecutedByHashedSerializer());
        traceabilityMapperModule.addSerializer(ExecutedBy.class, new ExecutedBySerializer());
        traceabilityMapperModule.addSerializer(From.class, new FromSerializer());
        traceabilityMapperModule.addSerializer(ExecutedAt.class, new ExecutedAtSerializer());
        traceabilityMapperModule.addSerializer(TraceId.class, new TraceIdSerializer());
        objectMapper.registerModule(traceabilityMapperModule);
        return objectMapper;
    }
}
