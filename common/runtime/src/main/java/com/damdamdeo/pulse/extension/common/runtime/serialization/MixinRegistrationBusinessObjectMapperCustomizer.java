package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public abstract class MixinRegistrationBusinessObjectMapperCustomizer implements BusinessObjectMapperCustomizer {

    @Override
    public void customize(final ObjectMapper objectMapper) {
        mixins().forEach((source, target) -> {
            try {
                objectMapper.addMixIn(
                        Thread.currentThread().getContextClassLoader().loadClass(source),
                        Thread.currentThread().getContextClassLoader().loadClass(target));
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException("Should not be here !", e);
            }
        });
    }

    protected abstract Map<String, String> mixins();
}
