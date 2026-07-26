package com.damdamdeo.pulse.extension.query.runtime.gdpr;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public final class OwnedBySerializer extends StdSerializer<OwnedBy> {

    public OwnedBySerializer() {
        super(OwnedBy.class);
    }

    @Override
    public void serialize(final OwnedBy value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeString(value.id());
    }
}
