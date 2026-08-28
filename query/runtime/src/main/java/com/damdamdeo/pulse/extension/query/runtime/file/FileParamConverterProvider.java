package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedAt;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedBy;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Provider
public class FileParamConverterProvider implements ParamConverterProvider {

    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.of(
            FileIdentifier.class, FileIdentifier::from,
            OwnedBy.class, OwnedBy::new,
            ContentLength.class, value -> new ContentLength(Long.parseLong(value)), // TODO exception not a Long
            Filename.class, Filename::new,
            UploadedAt.class, value -> new UploadedAt(ZonedDateTime.parse(value)), // TODO exception not a parseable ZonedDateTime
            Token.class, value -> new Token(UUID.fromString(value)), // TODO exception not an UUID
            // DownloadedBy.class, value -> new DownloadedBy(), TODO if needed - need processing
            DownloadedAt.class, value -> new DownloadedAt(ZonedDateTime.parse(value)) // TODO exception not a parseable ZonedDateTime
    );

    private static final Map<Class<?>, Function<Object, String>> SERIALIZERS = Map.of(
            FileIdentifier.class, value -> ((FileIdentifier) value).id(),
            OwnedBy.class, value -> ((OwnedBy) value).id(),
            ContentLength.class, value -> ((ContentLength) value).contentLength().toString(),
            Filename.class, value -> ((Filename) value).filename(),
            UploadedAt.class, value -> ((UploadedAt) value).at().toString(),
            UploadedBy.class, value -> ((UploadedBy) value).executedBy().value(),
            Token.class, value -> ((Token) value).value().toString(),
            DownloadedBy.class, value -> ((DownloadedBy) value).executedBy().value(),
            DownloadedAt.class, value -> ((DownloadedAt) value).at().toString()
    );

    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        final Function<String, ?> converter = CONVERTERS.get(rawType);
        final Function<Object, String> serializer = SERIALIZERS.get(rawType);
        if (converter == null) {
            return null;
        }
        return new ParamConverter<>() {

            @Override
            public T fromString(final String value) {
                Objects.requireNonNull(value);
                return rawType.cast(converter.apply(value));
            }

            @Override
            public String toString(final T value) {
                Objects.requireNonNull(value);
                return serializer.apply(value);
            }
        };
    }
}
