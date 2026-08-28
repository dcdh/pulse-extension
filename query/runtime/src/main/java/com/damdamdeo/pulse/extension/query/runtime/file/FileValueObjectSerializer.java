package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedAt;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedBy;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

public final class FileValueObjectSerializer extends JsonSerializer<Object> {

    private static final Map<Class<?>, Function<Object, Object>> SERIALIZERS = Map.of(
            FileIdentifier.class, value -> ((FileIdentifier) value).id(),
            Filename.class, value -> ((Filename) value).filename(),
            ContentLength.class, value -> ((ContentLength) value).contentLength(),
            UploadedAt.class, value -> ((UploadedAt) value).at().toString(),
            OwnedBy.class, value -> ((OwnedBy) value).id(),
            UploadedBy.class, value -> ((UploadedBy) value).executedBy().value(),
            Token.class, value -> ((Token) value).value().toString(),
            DownloadedBy.class, value -> ((DownloadedBy) value).executedBy().value(),
            DownloadedAt.class, value -> ((DownloadedAt) value).at().toString()
    );

    @Override
    public void serialize(final Object value, final JsonGenerator generator, final SerializerProvider serializers)
            throws IOException {
        final Function<Object, Object> serializer = SERIALIZERS.get(value.getClass());
        if (serializer == null) {
            throw JsonMappingException.from(generator, "Unable to serialize type: " + value.getClass().getName());
        }
        final Object serializedValue = serializer.apply(value);
        serializers.defaultSerializeValue(serializedValue, generator);
    }
}
