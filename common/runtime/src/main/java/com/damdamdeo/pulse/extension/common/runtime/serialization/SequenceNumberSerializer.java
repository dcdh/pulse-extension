package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SequenceNumberSerializer extends JsonSerializer<SequenceNumber> {

    @Override
    public void serialize(final SequenceNumber value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {
        gen.writeString(value.number());
    }
}
