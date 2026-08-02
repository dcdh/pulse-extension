package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.encryption.UnknownPassphraseException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class MessagingLiveNotifierPublisher<T> implements LiveNotifierPublisher<T> {
    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    public static final String EVENT_NAME = "event-name";
    public static final String CONTENT_TYPE = "content-type";

    public static final String CONTENT_TYPE_PREFIX = "application/vnd.";
    public static final String CONTENT_TYPE_SUFFIX = ".api+json";
    public static final String OWNED_BY = "owned-by";
    public static final String AUDIENCE = "audience";

    @Inject
    @Channel("live-notification-out")
    MutinyEmitter<byte[]> emitter;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    EncryptionService encryptionService;

    @Inject
    ExecutedByEncoder executedByEncoder;

    @Override
    public void publish(final String eventName, final T payload, final OwnedBy ownedBy, final Audience audience) throws PublicationException {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(audience);
        try {
            final byte[] jsonPayload = objectMapper.writeValueAsBytes(payload);
            final Encrypted<byte[]> encrypted = encryptionService.encrypt(new ByteArrayInputStream(jsonPayload), ownedBy,
                    encryptedPayload -> {
                        try (final InputStream payload1 = encryptedPayload.payload()) {
                            return new Encrypted<>(payload1.readAllBytes());
                        }
                    });

            final String contentType = CONTENT_TYPE_PREFIX + payload.getClass().getName() + CONTENT_TYPE_SUFFIX;
            emitter.sendMessageAndAwait(
                    Message.of(encrypted.payload()).addMetadata(
                            OutgoingKafkaRecordMetadata.<String>builder()
                                    .withHeaders(new RecordHeaders()
                                            .add(EVENT_NAME, eventName.getBytes())
                                            .add(CONTENT_TYPE, contentType.getBytes(StandardCharsets.UTF_8))
                                            .add(OWNED_BY, ownedBy.id().getBytes(StandardCharsets.UTF_8))
                                            .add(AUDIENCE, audience.encode(executedByEncoder, ownedBy).getBytes(StandardCharsets.UTF_8)))
                                    .build()));
        } catch (final EncryptionException exception) {
            if (exception.getCause() instanceof UnknownPassphraseException) {
                LOGGER.fine("Unknown passphrase for %s - notification will not be sent".formatted(ownedBy.id()));
            }
            throw new PublicationException(exception);
        } catch (final JsonProcessingException | UnableToEncodeException exception) {
            if (exception instanceof UnableToEncodeException && exception.getCause() instanceof UnknownPassphraseException) {
                LOGGER.fine("Unknown passphrase for %s - notification will not be sent".formatted(ownedBy.id()));
            } else {
                throw new PublicationException(exception);
            }
        }
    }
}
