package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.deserializer;

import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.Sensitive;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer.EncryptablePropertyWriter;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

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
        if (annotated.hasAnnotation(Sensitive.class)) {
            return new PropertyName(annotated.getName() + EncryptablePropertyWriter.ENCRYPTED_FIELD_SUFFIX);
        }
        return super.findNameForDeserialization(annotated);
    }

    @Override
    public Object findDeserializer(final Annotated annotated) {
        if (annotated.hasAnnotation(Sensitive.class)) {
            return new EncryptedDeserializer(pulseQueryConfig, decryptionService);
        }
        return super.findDeserializer(annotated);
    }
}
