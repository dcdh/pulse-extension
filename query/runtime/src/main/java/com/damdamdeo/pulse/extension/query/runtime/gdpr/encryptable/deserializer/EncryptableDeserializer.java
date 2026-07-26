package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.deserializer;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.Encryptable;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Objects;

public class EncryptableDeserializer<T extends Encryptable> extends StdDeserializer<T> {

    private final JavaType targetType;
    private final PulseQueryConfig pulseQueryConfig;
    private final DecryptionService decryptionService;

    public EncryptableDeserializer(final JavaType targetType,
                                   final PulseQueryConfig pulseQueryConfig,
                                   final DecryptionService decryptionService) {
        super(targetType.getRawClass());
        this.targetType = Objects.requireNonNull(targetType);
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.decryptionService = Objects.requireNonNull(decryptionService);
    }

    @Override
    public T deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        final String encrypted = p.getValueAsString();
        final byte[] decrypted;
        try {
            final MasterKey masterKey = new MasterKey(pulseQueryConfig.masterKey());
            final EncryptedPayload encryptedPayload = new EncryptedPayload(encrypted.getBytes());
            final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, masterKey.toPassphrase());
            decrypted = decryptedPayload.payload();
        } catch (final DecryptionException e) {
            throw JsonMappingException.from(p, "Unable to decrypt", e);
        }

        /*
         * On laisse Jackson créer le record
         */
        final JsonParser delegateParser = p.getCodec().getFactory().createParser(decrypted);
        delegateParser.nextToken();
        return ctxt.readValue(delegateParser, targetType);
    }
}
