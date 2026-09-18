package com.damdamdeo.pulse.extension.traceability.runtime.api.serialization;

import com.damdamdeo.pulse.extension.core.traceability.ExecutedAt;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public class ExecutedAtSerializer extends StdSerializer<ExecutedAt> {

    public ExecutedAtSerializer() {
        super(ExecutedAt.class);
    }

    @Override
    public void serialize(final ExecutedAt value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        provider.defaultSerializeValue(value.at(), gen);
    }
}
