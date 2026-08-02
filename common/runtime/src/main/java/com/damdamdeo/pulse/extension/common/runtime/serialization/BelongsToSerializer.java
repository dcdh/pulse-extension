package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public final class BelongsToSerializer extends StdSerializer<BelongsTo> {

    public BelongsToSerializer() {
        super(BelongsTo.class);
    }

    @Override
    public void serialize(final BelongsTo value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeString(value.id());
    }
}
