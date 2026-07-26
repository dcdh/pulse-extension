package com.damdamdeo.pulse.extension.query.runtime.gdpr.hashable;

import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.util.Objects;

public final class HashablePropertyWriter extends BeanPropertyWriter {

    private final Hasher hasher;

    public HashablePropertyWriter(final BeanPropertyWriter base, final Hasher hasher) {
        super(base);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public void serializeAsField(final Object bean, final JsonGenerator gen, final SerializerProvider provider) throws Exception {
        final Hashable hashable = (Hashable) get(bean);
        if (hashable == null) {
            return;
        }
        gen.writeStringField(getName() + "_hash", hasher.hash(hashable.value()));
    }
}
