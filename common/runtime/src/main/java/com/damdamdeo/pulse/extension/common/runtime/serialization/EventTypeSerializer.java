package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.event.EventType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public final class EventTypeSerializer extends StdSerializer<EventType> {

    public EventTypeSerializer() {
        super(EventType.class);
    }

    @Override
    public void serialize(final EventType value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeString(value.type());
    }
}
