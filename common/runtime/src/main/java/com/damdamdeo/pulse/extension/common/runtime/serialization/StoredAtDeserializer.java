package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.event.StoredAt;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import java.io.IOException;

public final class StoredAtDeserializer extends StdDeserializer<StoredAt> {

    public StoredAtDeserializer() {
        super(StoredAt.class);
    }

    @Override
    public StoredAt deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        return new StoredAt(InstantDeserializer.INSTANT.deserialize(p, ctxt));
    }
}
