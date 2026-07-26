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
            if (annotated.getRawType() == String.class) {
                return new StdDeserializer<String>(String.class) {
                    @Override
                    public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
                        return new String(decrypt(p));
                    }
                };
            } else if (annotated.getRawType() == Long.class) {
                return new StdDeserializer<Long>(Long.class) {
                    @Override
                    public Long deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
                        return Long.valueOf(new String(decrypt(p)));
                    }
                };
            } else if (annotated.getRawType() == Integer.class) {
                return new StdDeserializer<Integer>(Integer.class) {
                    @Override
                    public Integer deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
                        return Integer.valueOf(new String(decrypt(p)));
                    }
                };
            } else if (annotated.getRawType() == Double.class) {
                return new StdDeserializer<Double>(Double.class) {
                    @Override
                    public Double deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
                        return Double.valueOf(new String(decrypt(p)));
                    }
                };
            } else {
                throw new UnsupportedOperationException("Unsupported type: " + annotated.getRawType());
            }
        }
        return super.findDeserializer(annotated);
    }

    private byte[] decrypt(final JsonParser p) throws IOException {
        try {
            final MasterKey masterKey = new MasterKey(pulseQueryConfig.masterKey());
            final EncryptedPayload encryptedPayload = new EncryptedPayload(p.getBinaryValue());
            final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, masterKey.toPassphrase());
            return decryptedPayload.payload();
        } catch (final DecryptionException e) {
            throw JsonMappingException.from(p, "Unable to decrypt", e);
        }
    }
}
