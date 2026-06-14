package com.damdamdeo.pulse.extension.obfuscator.runtime.annotation;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ObfuscationBeanSerializerModifier extends BeanSerializerModifier {

    private final Obfuscator obfuscator;

    public ObfuscationBeanSerializerModifier(final Obfuscator obfuscator) {
        this.obfuscator = Objects.requireNonNull(obfuscator);
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(
            final SerializationConfig config,
            final BeanDescription beanDesc,
            final List<BeanPropertyWriter> beanProperties) {

        final List<BeanPropertyWriter> result = new ArrayList<>();
        for (BeanPropertyWriter writer : beanProperties) {
            if (writer.getAnnotation(Obfuscate.class) != null
                    && writer.getType().getRawClass() == String.class) {
                result.add(new ObfuscatedBeanPropertyWriter(writer, obfuscator));
            } else {
                result.add(writer);
            }
        }
        return result;
    }
}
