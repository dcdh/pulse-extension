package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer;

import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.Encrypted;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EncryptableSerializerModifier extends BeanSerializerModifier {

    private final PulseQueryConfig pulseQueryConfig;
    private final EncryptionService encryptionService;

    public EncryptableSerializerModifier(final PulseQueryConfig pulseQueryConfig,
                                         final EncryptionService encryptionService) {
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.encryptionService = Objects.requireNonNull(encryptionService);
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(final SerializationConfig config,
                                                     final BeanDescription beanDesc,
                                                     final List<BeanPropertyWriter> beanProperties) {
        final List<BeanPropertyWriter> result = new ArrayList<>();
        for (final BeanPropertyWriter writer : beanProperties) {
            final AnnotatedMember member = writer.getMember();
            final Encrypted encrypted = member.getAnnotation(Encrypted.class);
            if (encrypted != null) {
                result.add(new EncryptablePropertyWriter(writer, pulseQueryConfig, encryptionService));
            } else {
                result.add(writer);
            }
        }
        return result;
    }
}
