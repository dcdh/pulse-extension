package com.damdamdeo.pulse.extension.query.runtime.gdpr;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Objects;

public final class OwnedByDeserializer extends StdDeserializer<OwnedBy> {

    public OwnedByDeserializer() {
        super(OwnedBy.class);
    }

    @Override
    public OwnedBy deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        Objects.requireNonNull(p);
        Objects.requireNonNull(ctxt);
        return new OwnedBy(p.getValueAsString());
    }
}
