package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.util.Objects;

public final class AggregateIdDeserializer<T extends AggregateId> extends JsonDeserializer<T> implements ContextualDeserializer {

    private final JavaType targetType;

    public AggregateIdDeserializer() {
        targetType = null;
    }

    private AggregateIdDeserializer(final JavaType targetType) {
        this.targetType = Objects.requireNonNull(targetType);
    }

    @Override
    public T deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        final T id = p.getCodec().readValue(p, targetType);
        final AggregateIdCollector collector = (AggregateIdCollector) ctxt.getAttribute(AggregateIdCollector.class);
        if (collector != null) {
            collector.add(id);
        }
        return id;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType type = ctxt.getContextualType();
        return new AggregateIdDeserializer<>(type);
    }
}
