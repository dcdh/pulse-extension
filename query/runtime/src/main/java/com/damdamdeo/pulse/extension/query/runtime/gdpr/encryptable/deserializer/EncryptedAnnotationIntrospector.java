package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.deserializer;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.Encrypted;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer.EncryptablePropertyWriter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.io.IOException;
import java.util.Objects;

public final class EncryptedAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private final PulseQueryConfig pulseQueryConfig;
    private final DecryptionService decryptionService;

    public EncryptedAnnotationIntrospector(final PulseQueryConfig pulseQueryConfig,
                                           final DecryptionService decryptionService) {
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.decryptionService = Objects.requireNonNull(decryptionService);
    }

    @Override
    public PropertyName findNameForDeserialization(final Annotated annotated) {
        if (annotated.hasAnnotation(Encrypted.class)) {
            return new PropertyName(annotated.getName() + EncryptablePropertyWriter.ENCRYPTED_FIELD_SUFFIX);
        }
        return super.findNameForDeserialization(annotated);
    }

    @Override
    public Object findDeserializer(final Annotated annotated) {
        if (annotated.hasAnnotation(Encrypted.class)) {
            return new StdDeserializer<String>(String.class) {
                @Override
                public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
                    try {
                        final MasterKey masterKey = new MasterKey(pulseQueryConfig.masterKey());
                        final EncryptedPayload encryptedPayload = new EncryptedPayload(p.getBinaryValue());
                        final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, masterKey.toPassphrase());
                        return new String(decryptedPayload.payload());
                    } catch (final DecryptionException e) {
                        throw JsonMappingException.from(p, "Unable to decrypt", e);
                    }
                }
            };
        }
        return super.findDeserializer(annotated);
    }
}
