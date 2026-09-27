package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.util.Objects;

public final class AggregateIdDeserializer<A extends AggregateId> extends JsonDeserializer<A> implements ContextualDeserializer {

    private final JavaType targetType;

    public AggregateIdDeserializer() {
        targetType = null;
    }

    private AggregateIdDeserializer(final JavaType targetType) {
        this.targetType = Objects.requireNonNull(targetType);
    }

    @Override
    public A deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        final A id = p.getCodec().readValue(p, targetType);
        final AggregateIdCollector<A> collector = (AggregateIdCollector<A>) ctxt.getAttribute(AggregateIdCollector.class);
        if (collector != null) {
            collector.add(id);
        }
        return id;
    }

    @Override
    public JsonDeserializer<?> createContextual(final DeserializationContext ctxt, final BeanProperty property) throws JsonMappingException {
        final JavaType type = ctxt.getContextualType();
        return new AggregateIdDeserializer<>(type);
    }
}
