package com.damdamdeo.pulse.extension.query.runtime.mapper;

import com.damdamdeo.pulse.extension.core.query.Projection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
}
