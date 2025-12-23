package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.livenotifier.runtime.MessagingLiveNotifierPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
@Unremovable
public class MessagingConsumer {
    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    @Inject
    Event<NotifyEvent> notifyEventProducer;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DecryptionService decryptionService;

    @Transactional
    @Blocking
    @Incoming("live-notification-in")
    public void consume(final ConsumerRecord<Void, byte[]> consumerRecord) {
        final String eventName = new String(consumerRecord.headers()
                .lastHeader(MessagingLiveNotifierPublisher.EVENT_NAME).value());
        final OwnedBy ownedBy = Optional.ofNullable(consumerRecord.headers()
                        .lastHeader(MessagingLiveNotifierPublisher.OWNED_BY).value())
                .map(String::new)
                .map(OwnedBy::new)
                .orElse(null);
        final String className = extractClassName(consumerRecord.headers());
        Object payload = null;
        if (ownedBy != null) {
            try {
                final DecryptedPayload decryptedPayload = decryptionService.decrypt(new EncryptedPayload(consumerRecord.value()), ownedBy);
                payload = mapToObject(decryptedPayload.payload(), className);
            } catch (final UnknownPassphraseException unknownPassphraseException) {
                LOGGER.fine("Unknown passphrase for %s - notification will not be sent".formatted(ownedBy.id()));
            } catch (final DecryptionException decryptionException) {
                LOGGER.fine("Fail to decrypt for %s %s - notification will not be sent".formatted(ownedBy.id(), decryptionException.getMessage()));
            }
        } else {
            payload = mapToObject(consumerRecord.value(), className);
        }
        if (payload != null) {
            notifyEventProducer.fire(new NotifyEvent(eventName, payload, ownedBy));
        }
    }

    private String extractClassName(final Headers headers) {
        return Optional.ofNullable(headers.lastHeader(MessagingLiveNotifierPublisher.CONTENT_TYPE))
                .map(Header::value)
                .map(String::new)
                .filter(contentType -> contentType.startsWith(MessagingLiveNotifierPublisher.CONTENT_TYPE_PREFIX)
                        && contentType.endsWith(MessagingLiveNotifierPublisher.CONTENT_TYPE_SUFFIX))
                .map(contentType -> contentType.substring(MessagingLiveNotifierPublisher.CONTENT_TYPE_PREFIX.length(),
                        contentType.length() - MessagingLiveNotifierPublisher.CONTENT_TYPE_SUFFIX.length()))
                .orElseThrow(() -> new IllegalStateException("Missing event type in headers"));
    }

    private Object mapToObject(final byte[] data, final String className) {
        try {
            final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return objectMapper.readValue(data, clazz);
        } catch (final IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize Kafka message", e);
        }
    }
}
