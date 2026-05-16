package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class SequenceNumberDeserializer extends JsonDeserializer<SequenceNumber> {

    @Override
    public SequenceNumber deserialize(final JsonParser p,
                                      final DeserializationContext ctxt) throws IOException {
        return new SequenceNumber(p.getValueAsString());
    }
}
