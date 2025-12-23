package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class MessagingLiveNotifierPublisher<T> implements LiveNotifierPublisher<T> {

    public static final String EVENT_NAME = "event-name";
    public static final String CONTENT_TYPE = "content-type";

    public static final String CONTENT_TYPE_PREFIX = "application/vnd.";
    public static final String CONTENT_TYPE_SUFFIX = ".api+json";
    public static final String OWNED_BY = "owned-by";

    @Inject
    @Channel("live-notification-out")
    MutinyEmitter<T> emitter;

    @Override
    public void publish(final String eventName, final T payload, final OwnedBy ownedBy) {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(payload);
        final String contentType = CONTENT_TYPE_PREFIX + payload.getClass().getName() + CONTENT_TYPE_SUFFIX;
        emitter.sendMessageAndAwait(
                Message.of(payload).addMetadata(
                        OutgoingKafkaRecordMetadata.<String>builder()
                                .withHeaders(new RecordHeaders()
                                        .add(EVENT_NAME, eventName.getBytes())
                                        .add(CONTENT_TYPE, contentType.getBytes(StandardCharsets.UTF_8))
                                        .add(OWNED_BY, ownedBy == null ? null : ownedBy.id().getBytes(StandardCharsets.UTF_8)))
                                .build()));
    }

    @Override
    public void publish(final String eventName, final T payload) {
        publish(eventName, payload, null);
    }
}
