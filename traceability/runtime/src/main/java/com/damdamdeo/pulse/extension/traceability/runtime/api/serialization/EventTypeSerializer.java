package com.damdamdeo.pulse.extension.traceability.runtime.api.serialization;

import com.damdamdeo.pulse.extension.core.event.EventType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public class EventTypeSerializer extends StdSerializer<EventType> {

    public EventTypeSerializer() {
        super(EventType.class);
    }

    @Override
    public void serialize(final EventType value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeString(value.type());
    }
}
