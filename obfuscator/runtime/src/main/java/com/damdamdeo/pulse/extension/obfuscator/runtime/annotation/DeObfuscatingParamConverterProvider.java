package com.damdamdeo.pulse.extension.obfuscator.runtime.annotation;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnknownObfuscatedException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.logging.Logger;

@Provider
@ApplicationScoped
@Priority(Priorities.USER - 100) // Ensure DeObfuscatingParamConverterProvider will be called first!
public class DeObfuscatingParamConverterProvider implements ParamConverterProvider {

    static final Logger LOGGER = Logger.getLogger(DeObfuscatingParamConverterProvider.class.getName());

    @Inject
    Obfuscator obfuscator;

    // Inject all ParamConverterProvider available inside the application
    @Inject
    @Any
    Instance<ParamConverterProvider> converterProviders;

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        boolean deObfuscate = Arrays.stream(annotations)
                .anyMatch(a -> a.annotationType() == DeObfuscate.class);

        if (!deObfuscate) {
            return null;
        }

        // Remove annotation on delegate
        final Annotation[] annotationsWithoutDeObfuscate = Arrays.stream(annotations)
                .filter(a -> a.annotationType() != DeObfuscate.class)
                .toArray(Annotation[]::new);

        // Seek for delegate converter (e.g. TodoIdParamConverterProvider)
        ParamConverter<T> delegate = null;
        for (final ParamConverterProvider provider : converterProviders) {
            // avoid calling himself
            if (provider instanceof DeObfuscatingParamConverterProvider) {
                continue;
            }
            delegate = provider.getConverter(rawType, genericType, annotationsWithoutDeObfuscate);
            if (delegate != null) {
                break; // found
            }
        }

        final ParamConverter<T> finalDelegate = delegate;

        return new ParamConverter<>() {

            @Override
            public T fromString(final String value) {
                try {
                    if (value == null) {
                        return null;
                    }
                    final String deObfuscatedValue = obfuscator.deObfuscate(value);

                    // use found delegate (e.g. TodoId)
                    if (finalDelegate != null) {
                        return finalDelegate.fromString(deObfuscatedValue);
                    }

                    // Otherwise, use String converter
                    if (rawType == String.class) {
                        return rawType.cast(deObfuscatedValue);
                    }

                    throw new IllegalArgumentException("No ParamConverter found for type: " + rawType.getName());
                } catch (final UnableToDeObfuscateException | UnknownObfuscatedException exception) {
                    LOGGER.severe("Unable to deobfuscate parameter: %s - endpoint will return a 404 response !".formatted(value));
                    throw new IllegalArgumentException("Unable to deobfuscate parameter", exception);
                }
            }

            @Override
            public String toString(final T value) {
                if (value == null) {
                    return null;
                }
                if (finalDelegate != null) {
                    return finalDelegate.toString(value);
                }
                return value.toString();
            }
        };
    }
}
