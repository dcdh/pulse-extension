package com.damdamdeo.pulse.extension.traceability.runtime.api.deserializer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public class AggregateIdSerializer extends StdSerializer<AggregateId> {

    public AggregateIdSerializer() {
        super(AggregateId.class);
    }

    @Override
    public void serialize(final AggregateId value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeString(value.id());
    }
}
