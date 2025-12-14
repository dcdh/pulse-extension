package com.damdamdeo.pulse.extension.consumer.runtime.notification;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.notifier.NotifierListenerException;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.UnknownPassphraseException;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.logging.Log;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractNotifierListener<E> {

    @Inject
    Event<NotifyEvent> notifyEventProducer;

    DecryptionService decryptionService;

    DecryptedPayloadToPayloadMapper<JsonNode> decryptedPayloadToPayloadMapper;

    AggregateRootLoader<JsonNode> aggregateRootLoader;

    protected void handleMessage(final FromApplication fromApplication,
                                 final EventKey eventKey,
                                 final EventValue eventValue) {
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        Log.debug("Consuming from application '%s' with key '%s' and value '%s'".formatted(fromApplication.value(), eventKey, eventValue));

        final AggregateRootType aggregateRootType = eventKey.toAggregateRootType();
        final AggregateId aggregateId = eventKey.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = eventKey.toCurrentVersionInConsumption();
        final Instant creationDate = eventValue.toCreationDate();
        final EventType eventType = eventValue.toEventType();
        final EncryptedPayload encryptedPayload = eventValue.toEncryptedEventPayload();
        final OwnedBy ownedBy = eventValue.toOwnedBy();
        DecryptablePayload<JsonNode> decryptableEventPayload;
        try {
            final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, ownedBy);
            final JsonNode decryptedEventPayload = decryptedPayloadToPayloadMapper.map(decryptedPayload);
            decryptableEventPayload = DecryptablePayload.ofDecrypted(decryptedEventPayload);
        } catch (final UnknownPassphraseException unknownPassphraseException) {
            decryptableEventPayload = DecryptablePayload.ofUndecryptable();
        } catch (IOException e) {
            throw new NotifierListenerException(fromApplication, aggregateId, aggregateRootType, currentVersionInConsumption, eventType, e);
        }
        final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier = () -> aggregateRootLoader.getByApplicationNamingAndAggregateRootTypeAndAggregateId(fromApplication, aggregateRootType, aggregateId);

        if (filter(fromApplication, aggregateRootType, aggregateId, currentVersionInConsumption, creationDate,
                eventType, encryptedPayload, ownedBy, decryptableEventPayload, aggregateRootLoadedSupplier)) {
            final E eventToPush = mapTo(fromApplication, aggregateRootType, aggregateId, currentVersionInConsumption, creationDate,
                    eventType, encryptedPayload, ownedBy, decryptableEventPayload, aggregateRootLoadedSupplier);
            notifyEventProducer.fireAsync(new NotifyEvent(eventName(), getClazz(), eventToPush));
        }
    }

    protected abstract boolean filter(FromApplication fromApplication,
                                      AggregateRootType aggregateRootType,
                                      AggregateId aggregateId,
                                      CurrentVersionInConsumption currentVersionInConsumption,
                                      Instant creationDate,
                                      EventType eventType,
                                      EncryptedPayload encryptedPayload,
                                      OwnedBy ownedBy,
                                      DecryptablePayload<JsonNode> decryptableEventPayload,
                                      Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier);

    protected abstract E mapTo(FromApplication fromApplication,
                               AggregateRootType aggregateRootType,
                               AggregateId aggregateId,
                               CurrentVersionInConsumption currentVersionInConsumption,
                               Instant creationDate,
                               EventType eventType,
                               EncryptedPayload encryptedPayload,
                               OwnedBy ownedBy,
                               DecryptablePayload<JsonNode> decryptableEventPayload,
                               Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier);

    protected abstract String eventName();

    protected Class<E> getClazz() {
        throw new IllegalStateException("Should be autogenerated");
    }
}
