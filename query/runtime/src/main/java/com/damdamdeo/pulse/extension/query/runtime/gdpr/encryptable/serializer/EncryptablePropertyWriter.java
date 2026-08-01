package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.encryption.MasterKey;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public final class EncryptablePropertyWriter extends BeanPropertyWriter {

    public static final String ENCRYPTED_FIELD_SUFFIX = "_encrypted";

    private final PulseQueryConfig pulseQueryConfig;
    private final EncryptionService encryptionService;

    public EncryptablePropertyWriter(final BeanPropertyWriter base,
                                     final PulseQueryConfig pulseQueryConfig,
                                     final EncryptionService encryptionService) {
        super(base);
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.encryptionService = Objects.requireNonNull(encryptionService);
    }

    @Override
    public void serializeAsField(final Object bean, final JsonGenerator gen, final SerializerProvider provider) throws Exception {
        final Object encryptable = get(bean);
        if (encryptable == null) {
            return;
        }
        final TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false);
        final JsonSerializer<Object> delegateSerializer = provider.findValueSerializer(encryptable.getClass(), this);
        delegateSerializer.serialize(encryptable, buffer, provider);

        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try (final JsonGenerator bufferGen = gen.getCodec().getFactory().createGenerator(bytesOut)) {
            buffer.serialize(bufferGen);
        }

        byte[] jsonBytes = bytesOut.toByteArray();
        // remove '"' at start and '"' at end of json if present. We do not want to encrypt them.
        // Sample: a deserilization of a LocalDateTime having this String value '"2026-07-26T20:47:15"' will not work. It must be '2026-07-26T20:47:15'.
        if (jsonBytes.length >= 2
                && jsonBytes[0] == '"'
                && jsonBytes[jsonBytes.length - 1] == '"') {
            jsonBytes = Arrays.copyOfRange(jsonBytes, 1, jsonBytes.length - 1);
        }

        final Passphrase passphrase = new MasterKey(pulseQueryConfig.masterKey()).toPassphrase();
        final Encrypted<byte[]> encrypted = encryptionService.encrypt(new ByteArrayInputStream(jsonBytes), passphrase,
                encryptedPayload -> {
                    try (final InputStream payload1 = encryptedPayload.payload()) {
                        return new Encrypted<>(payload1.readAllBytes());
                    }
                });
        gen.writeBinaryField(getName() + ENCRYPTED_FIELD_SUFFIX, encrypted.payload());
    }
}
