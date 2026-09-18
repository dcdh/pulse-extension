package com.damdamdeo.pulse.extension.obfuscator.runtime.annotation;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.util.Objects;

public class ObfuscatedBeanPropertyWriter extends BeanPropertyWriter {

    private final BeanPropertyWriter delegate;
    private final Obfuscator obfuscator;

    public ObfuscatedBeanPropertyWriter(final BeanPropertyWriter delegate, final Obfuscator obfuscator) {
        super(delegate);
        this.delegate = Objects.requireNonNull(delegate);
        this.obfuscator = Objects.requireNonNull(obfuscator);
    }

    @Override
    public void serializeAsField(final Object bean, final JsonGenerator gen, final SerializerProvider prov) throws Exception {
        final Object value = delegate.get(bean);
        if (value instanceof String stringValue) {
            gen.writeFieldName(delegate.getName());
            gen.writeString(obfuscator.obfuscate(stringValue));
            return;
        } else if (value instanceof AggregateId aggregateId) {
            gen.writeFieldName(delegate.getName());
            gen.writeString(obfuscator.obfuscate(aggregateId.id()));
            return;
        }
        delegate.serializeAsField(bean, gen, prov);
    }
}
