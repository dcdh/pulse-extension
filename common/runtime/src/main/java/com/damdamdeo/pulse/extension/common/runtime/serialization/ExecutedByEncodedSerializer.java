package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public final class ExecutedByEncodedSerializer extends StdSerializer<ExecutedByEncoded> {

    public ExecutedByEncodedSerializer() {
        super(ExecutedByEncoded.class);
    }

    @Override
    public void serialize(final ExecutedByEncoded value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeString(value.encoded());
    }
}
