package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import com.damdamdeo.pulse.extension.livenotifier.runtime.MessagingLiveNotifierPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public abstract class JacksonHeaderBasedDeserializer implements Deserializer<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JacksonHeaderBasedDeserializer() {
        mixins().forEach((target, mixinSource) -> {
            try {
                OBJECT_MAPPER.addMixIn(
                        Thread.currentThread().getContextClassLoader().loadClass(target),
                        Thread.currentThread().getContextClassLoader().loadClass(mixinSource));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        throw new UnsupportedOperationException("Should not be called - unknown type");
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) {
            return null;
        }
        return extractClassName(headers)
                .map(className -> mapToObject(data, className))
                .orElseThrow(() -> new IllegalStateException("Missing event type in headers"));
    }

    private static Object mapToObject(final byte[] data, final String className) {
        try {
            final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return OBJECT_MAPPER.readValue(data, clazz);
        } catch (final IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize Kafka message", e);
        }
    }

    private Optional<String> extractClassName(final Headers headers) {
        return Optional.ofNullable(headers.lastHeader(MessagingLiveNotifierPublisher.CONTENT_TYPE))
                .map(Header::value)
                .map(String::new)
                .filter(contentType -> contentType.startsWith(MessagingLiveNotifierPublisher.CONTENT_TYPE_PREFIX)
                        && contentType.endsWith(MessagingLiveNotifierPublisher.CONTENT_TYPE_SUFFIX))
                .map(contentType -> contentType.substring(MessagingLiveNotifierPublisher.CONTENT_TYPE_PREFIX.length(),
                        contentType.length() - MessagingLiveNotifierPublisher.CONTENT_TYPE_SUFFIX.length()));
    }

    abstract protected Map<String, String> mixins();
}
