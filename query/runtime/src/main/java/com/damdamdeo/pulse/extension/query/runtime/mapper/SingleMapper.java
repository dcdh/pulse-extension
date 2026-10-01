package com.damdamdeo.pulse.extension.query.runtime.mapper;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.SingleResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

public interface SingleMapper<P> {

    P map(String json, ObjectMapper objectMapper) throws IOException;

    static <A extends AggregateId, P extends Projection<A>> SingleMapper<P> single(final TypeReference<P> typeReference) {
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            return objectMapper.readValue(json, typeReference);
        };
    }

    static <A extends AggregateId, P extends Projection<A>> SingleMapper<SingleResult<A, P>> resultSingle(final Class<A> clazz, final TypeReference<P> typeReference) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            final JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);
            final P projection = objectMapper.readValue(json, javaType);
            return new SingleResult<>(projection);
        };
    }
}
