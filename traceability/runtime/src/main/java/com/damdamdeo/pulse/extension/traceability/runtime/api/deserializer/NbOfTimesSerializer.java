package com.damdamdeo.pulse.extension.traceability.runtime.api.deserializer;

import com.damdamdeo.pulse.extension.core.traceability.NbOfTimes;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public class NbOfTimesSerializer extends StdSerializer<NbOfTimes> {

    public NbOfTimesSerializer() {
        super(NbOfTimes.class);
    }

    @Override
    public void serialize(final NbOfTimes value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeNumber(value.times());
    }
}
