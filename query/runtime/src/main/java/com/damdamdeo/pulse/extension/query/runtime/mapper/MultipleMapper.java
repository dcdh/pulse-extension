package com.damdamdeo.pulse.extension.query.runtime.mapper;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.Result;
import com.damdamdeo.pulse.extension.query.runtime.AggregateIdCollector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

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

    static <A extends AggregateId, P extends Projection<A>> MultipleMapper<Result<A, P>> resultMultiple(final Class<A> clazz, final TypeReference<List<P>> typeReference) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(typeReference);
        return (json, objectMapper) -> {
            Objects.requireNonNull(json);
            Objects.requireNonNull(objectMapper);
            final AggregateIdCollector<A> collector = new AggregateIdCollector<>(clazz);
            final JavaType javaType = objectMapper
                    .getTypeFactory()
                    .constructType(typeReference);
            final ObjectReader reader = objectMapper
                    .readerFor(javaType)
                    .withAttribute(AggregateIdCollector.class, collector);
            final List<P> projection = reader.readValue(json);
            return Result.of(projection, collector.aggregateId());
        };
    }
}
