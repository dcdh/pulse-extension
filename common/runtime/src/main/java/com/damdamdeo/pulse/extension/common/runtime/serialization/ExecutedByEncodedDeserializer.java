package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public final class ExecutedByEncodedDeserializer extends StdDeserializer<ExecutedByEncoded> {

    public ExecutedByEncodedDeserializer() {
        super(ExecutedByEncoded.class);
    }

    @Override
    public ExecutedByEncoded deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        return new ExecutedByEncoded(p.getValueAsString());
    }
}
