package com.damdamdeo.pulse.extension.query.runtime.mapper;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.MultipleResult;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public interface MultipleMapper<P> {

    P map(String json, ObjectMapper objectMapper) throws IOException;

    static <A extends AggregateId, P extends Projection<A>> MultipleMapper<List<P>> multiple(final TypeReference<List<P>> typeReference) {
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            return objectMapper.readValue(json, typeReference);
        };
    }

    static <A extends AggregateId, P extends Projection<A>> MultipleMapper<MultipleResult<A, P>> resultMultiple(final Class<A> clazz, final TypeReference<List<P>> typeReference) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            final JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);
            final List<P> projection = objectMapper.readValue(json, javaType);
            return new MultipleResult<>(projection);
        };
    }
}
