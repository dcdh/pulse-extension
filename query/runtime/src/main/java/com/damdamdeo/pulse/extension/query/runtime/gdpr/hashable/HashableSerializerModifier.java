package com.damdamdeo.pulse.extension.query.runtime.gdpr.hashable;

import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.Encrypted;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HashableSerializerModifier extends BeanSerializerModifier {

    private final Hasher hasher;

    public HashableSerializerModifier(final Hasher hasher) {
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(final SerializationConfig config,
                                                     final BeanDescription beanDesc,
                                                     final List<BeanPropertyWriter> beanProperties) {
        final List<BeanPropertyWriter> result = new ArrayList<>();
        for (final BeanPropertyWriter writer : beanProperties) {
            final AnnotatedMember member = writer.getMember();
            final Encrypted encrypted = member.getAnnotation(Encrypted.class);
            if (encrypted != null && Encrypted.Mode.ENCRYPT_AND_SEARCH_BY_HASH.equals(encrypted.value())) {
                result.add(new HashablePropertyWriter(writer, hasher));
                result.add(writer);
            } else {
                result.add(writer);
            }
        }
        return result;
    }
}
