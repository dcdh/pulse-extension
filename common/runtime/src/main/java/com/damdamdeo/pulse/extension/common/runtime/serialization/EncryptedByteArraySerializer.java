package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public final class EncryptedByteArraySerializer extends StdSerializer<Encrypted<byte[]>> {

    public EncryptedByteArraySerializer() {
        super(Encrypted.class, false);
    }

    @Override
    public void serialize(final Encrypted<byte[]> value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        provider.defaultSerializeValue(value.payload(), gen);
    }
}
