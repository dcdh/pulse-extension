package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public interface Mapper<T> {

    T map(String json, ObjectMapper objectMapper) throws IOException;

    static <T extends Projection> Mapper<T> single(final TypeReference<T> typeReference) {
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            return objectMapper.readValue(json, typeReference);
        };
    }

    static <T extends Projection> Mapper<List<T>> multiple(final TypeReference<List<T>> typeReference) {
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            return objectMapper.readValue(json, typeReference);
        };
    }

    static <T extends Projection> Mapper<Result<T>> resultSingle(final TypeReference<T> typeReference) {
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            final AggregateIdCollector collector = new AggregateIdCollector();
            final JavaType javaType = objectMapper
                    .getTypeFactory()
                    .constructType(typeReference);
            final ObjectReader reader = objectMapper
                    .readerFor(javaType)
                    .withAttribute(AggregateIdCollector.class, collector);
            final T projection = reader.readValue(json);
            return Result.of(projection, collector.aggregateId());
        };
    }

    static <T extends Projection> Mapper<Result<T>> resultMultiple(final TypeReference<List<T>> typeReference) {
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            final AggregateIdCollector collector = new AggregateIdCollector();
            final JavaType javaType = objectMapper
                    .getTypeFactory()
                    .constructType(typeReference);
            final ObjectReader reader = objectMapper
                    .readerFor(javaType)
                    .withAttribute(AggregateIdCollector.class, collector);
            final List<T> projection = reader.readValue(json);
            return Result.of(projection, collector.aggregateId());
        };
    }
}
