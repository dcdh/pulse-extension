package com.damdamdeo.pulse.extension.it.query.todo;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.Sensitive;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

import java.util.Objects;

@Singleton
public class TodoObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(final ObjectMapper objectMapper) {
        internal(objectMapper);
    }

    public static ObjectMapper internal(final ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        objectMapper.addMixIn(Description.class, DescriptionMixin.class);
        objectMapper.addMixIn(TodoProjection.class, TodoProjectionMixin.class);
        return objectMapper;
    }

    public abstract static class TodoProjectionMixin {

        @JsonIgnore
        public abstract boolean isInProgress();
    }

    public record DescriptionMixin(@Sensitive String description) {
    }
}
