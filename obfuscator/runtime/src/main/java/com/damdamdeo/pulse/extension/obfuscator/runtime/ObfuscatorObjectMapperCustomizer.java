package com.damdamdeo.pulse.extension.obfuscator.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.obfuscator.runtime.annotation.ObfuscationBeanSerializerModifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.arc.Unremovable;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Objects;

@Singleton
@Unremovable
public class ObfuscatorObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Inject
    Obfuscator obfuscator;

    @Override
    public void customize(ObjectMapper objectMapper) {
        customizeObjectMapper(objectMapper, obfuscator);
    }

    public static void customizeObjectMapper(final ObjectMapper objectMapper, final Obfuscator obfuscator) {
        Objects.requireNonNull(objectMapper);
        Objects.requireNonNull(obfuscator);
        final SimpleModule obfuscatorModule = new SimpleModule();
        obfuscatorModule.addSerializer(AggregateId.class, new AggregateIdObfuscatorSerializer(obfuscator));
        obfuscatorModule.setSerializerModifier(new ObfuscationBeanSerializerModifier(obfuscator));
        objectMapper.registerModule(obfuscatorModule);
    }
}
