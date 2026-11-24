package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.event.EventType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public final class EventTypeDeserializer extends StdDeserializer<EventType> {

    public EventTypeDeserializer() {
        super(EventType.class);
    }

    @Override
    public EventType deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        return new EventType(p.getValueAsString());
    }
}
