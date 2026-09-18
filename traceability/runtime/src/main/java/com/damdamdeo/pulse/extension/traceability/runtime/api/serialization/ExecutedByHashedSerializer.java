package com.damdamdeo.pulse.extension.traceability.runtime.api.serialization;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public class ExecutedByHashedSerializer extends StdSerializer<ExecutedByHashed> {

    public ExecutedByHashedSerializer() {
        super(ExecutedByHashed.class);
    }

    @Override
    public void serialize(final ExecutedByHashed value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeString(value.hashed());
    }
}

