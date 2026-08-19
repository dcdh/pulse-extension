package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.deserializer;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EncryptedDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private final PulseQueryConfig pulseQueryConfig;
    private final DecryptionService decryptionService;
    private final JsonDeserializer<Object> delegate;

    public EncryptedDeserializer(final PulseQueryConfig pulseQueryConfig,
                                 final DecryptionService decryptionService) {
        this(pulseQueryConfig, decryptionService, null);
    }

    private EncryptedDeserializer(final PulseQueryConfig pulseQueryConfig,
                                  final DecryptionService decryptionService,
                                  final JsonDeserializer<Object> delegate) {
        this.pulseQueryConfig = pulseQueryConfig;
        this.decryptionService = decryptionService;
        this.delegate = delegate;
    }

    @Override
    public JsonDeserializer<?> createContextual(final DeserializationContext ctxt,
                                                final BeanProperty property) throws JsonMappingException {
        final JavaType type = (property != null) ? property.getType() : ctxt.getContextualType();
        final JsonDeserializer<Object> defaultDelegate = ctxt.findContextualValueDeserializer(type, property);
        return new EncryptedDeserializer(pulseQueryConfig, decryptionService, defaultDelegate);
    }

    @Override
    public Object deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        final byte[] decryptedBytes = decrypt(p);
        final String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);

        try (final TokenBuffer tb = new TokenBuffer(p.getCodec(), false)) {
            tb.writeString(decryptedString);

            try (final JsonParser delegateParser = tb.asParser(p)) {
                delegateParser.nextToken();

                if (delegate != null) {
                    return delegate.deserialize(delegateParser, ctxt);
                }
                return delegateParser.getText();
            }
        }
    }

    private byte[] decrypt(final JsonParser p) throws IOException {
        try {
            final MasterKey masterKey = new MasterKey(pulseQueryConfig.masterKey());
            final Encrypted<byte[]> encrypted = Encrypted.of(p.getBinaryValue());
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(encrypted, masterKey.toPassphrase());
            return decrypted.payload();
        } catch (final DecryptionException e) {
            throw JsonMappingException.from(p, "Unable to decrypt", e);
        }
    }
}
