package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public final class EncryptedByteArrayDeserializer extends StdDeserializer<Encrypted<byte[]>> {

    public EncryptedByteArrayDeserializer() {
        super(Encrypted.class);
    }

    @Override
    public Encrypted<byte[]> deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        return Encrypted.of(p.getBinaryValue(ctxt.getBase64Variant()));
    }
}
