package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer;

import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.encryption.MasterKey;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.Encryptable;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.nio.charset.StandardCharsets;
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
        final Encryptable encryptable = (Encryptable) get(bean);
        if (encryptable == null) {
            return;
        }
        final Passphrase passphrase = new MasterKey(pulseQueryConfig.masterKey()).toPassphrase();
        byte[] payload = encryptionService.encrypt(encryptable.value().getBytes(StandardCharsets.UTF_8), passphrase).payload();
        gen.writeBinaryField(getName() + ENCRYPTED_FIELD_SUFFIX, payload);
    }
}
