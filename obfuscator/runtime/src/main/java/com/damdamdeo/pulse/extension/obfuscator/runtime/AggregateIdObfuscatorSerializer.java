package com.damdamdeo.pulse.extension.obfuscator.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToObfuscateException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Objects;

public class AggregateIdObfuscatorSerializer extends JsonSerializer<AggregateId> {

    private final Obfuscator obfuscator;

    public AggregateIdObfuscatorSerializer(final Obfuscator obfuscator) {
        this.obfuscator = Objects.requireNonNull(obfuscator);
    }

    @Override
    public void serialize(final AggregateId aggregateId, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        Objects.requireNonNull(aggregateId);
        try {
            gen.writeString(obfuscator.obfuscate(aggregateId.id()));
        } catch (final UnableToObfuscateException e) {
            throw new IOException(e);
        }
    }
}
