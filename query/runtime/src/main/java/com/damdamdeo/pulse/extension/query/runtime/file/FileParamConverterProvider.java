package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;

@Provider
@ApplicationScoped
public class FileParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (!FileIdentifier.class.equals(rawType)) {
            return null;
        }

        return (ParamConverter<T>) new ParamConverter<FileIdentifier>() {

            @Override
            public FileIdentifier fromString(final String value) {
                Objects.requireNonNull(value);
                return FileIdentifier.from(value);
            }

            @Override
            public String toString(final FileIdentifier value) {
                Objects.requireNonNull(value);
                return value.id();
            }
        };
    }
}
