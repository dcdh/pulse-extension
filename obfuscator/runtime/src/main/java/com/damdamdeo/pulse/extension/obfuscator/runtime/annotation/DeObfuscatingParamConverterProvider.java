package com.damdamdeo.pulse.extension.obfuscator.runtime.annotation;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

@Provider
@ApplicationScoped
public class DeObfuscatingParamConverterProvider implements ParamConverterProvider {

    @Inject
    Obfuscator obfuscator;

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (rawType != String.class) {
            return null;
        }
        final boolean deObfuscate = Arrays.stream(annotations)
                .anyMatch(a -> a.annotationType() == DeObfuscate.class);
        if (!deObfuscate) {
            return null;
        }
        return (ParamConverter<T>) new DeObfuscatingStringParamConverter(obfuscator);
    }
}
