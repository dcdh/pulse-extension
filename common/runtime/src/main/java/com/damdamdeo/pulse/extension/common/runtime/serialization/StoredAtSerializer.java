package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.event.StoredAt;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public final class StoredAtSerializer extends StdSerializer<StoredAt> {

    public StoredAtSerializer() {
        super(StoredAt.class);
    }

    @Override
    public void serialize(final StoredAt value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeString(value.at().toString());
    }
}
