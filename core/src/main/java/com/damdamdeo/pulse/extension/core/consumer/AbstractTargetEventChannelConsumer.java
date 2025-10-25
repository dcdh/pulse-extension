package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class AbstractTargetEventChannelConsumer<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final DecryptionService decryptionService;
    private final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper;
    private final AggregateRootLoader<T> aggregateRootLoader;
    private final AsyncEventChannelMessageHandlerProvider<T> asyncEventChannelMessageHandlerProvider;
    private final IdempotencyRepository idempotencyRepository;
    private final SequentialEventChecker sequentialEventChecker;

    public AbstractTargetEventChannelConsumer(final DecryptionService decryptionService,
                                              final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper,
                                              final AggregateRootLoader<T> aggregateRootLoader,
                                              final AsyncEventChannelMessageHandlerProvider<T> asyncEventChannelMessageHandlerProvider,
                                              final IdempotencyRepository idempotencyRepository,
                                              final SequentialEventChecker sequentialEventChecker) {
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.decryptedPayloadToPayloadMapper = Objects.requireNonNull(decryptedPayloadToPayloadMapper);
        this.aggregateRootLoader = Objects.requireNonNull(aggregateRootLoader);
        this.asyncEventChannelMessageHandlerProvider = Objects.requireNonNull(asyncEventChannelMessageHandlerProvider);
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository);
        this.sequentialEventChecker = Objects.requireNonNull(sequentialEventChecker);
    }

    protected void handleMessage(final Target target, final EventKey eventKey, final EventRecord eventRecord) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventRecord);
        LOGGER.fine("Consuming on target '%s' with key '%s' and value '%s'".formatted(target.name(), eventKey, eventRecord));
        if (!eventKey.aggregateRootId().equals(eventRecord.aggregateRootId())
                || !eventKey.aggregateRootType().equals(eventRecord.aggregateRootType())
                || !eventKey.version().equals(eventRecord.version())) {
            throw new IllegalArgumentException("Key mismatch with payload !");
        }
        final AggregateRootType aggregateRootType = eventRecord.toAggregateRootType();
        final AggregateId aggregateId = eventRecord.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = eventRecord.toCurrentVersionInConsumption();

        idempotencyRepository.findLastAggregateVersionBy(target, aggregateRootType, aggregateId)
                .filter(last -> last.isBelow(currentVersionInConsumption))
                .ifPresentOrElse((lastConsumedAggregateVersion) -> {
                            sequentialEventChecker.check(lastConsumedAggregateVersion, currentVersionInConsumption);
                            asyncEventChannelMessageHandlerProvider.provideForTarget(target)
                                    .forEach(asyncEventChannelMessageHandler -> {
                                        final Instant creationDate = eventRecord.toCreationDate();
                                        final EventType eventType = eventRecord.toEventType();
                                        final EncryptedPayload encryptedPayload = eventRecord.toEncryptedEventPayload();
                                        final OwnedBy ownedBy = eventRecord.toOwnedBy();
                                        try {
                                            final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, ownedBy);
                                            final T eventPayload = decryptedPayloadToPayloadMapper.map(decryptedPayload);
                                            final Supplier<AggregateRootLoaded<T>> aggregateRootSupplier = () -> aggregateRootLoader.getByAggregateRootTypeAndAggregateId(aggregateRootType, aggregateId);
                                            asyncEventChannelMessageHandler.handleMessage(target, aggregateId, aggregateRootType, currentVersionInConsumption, creationDate, eventType, encryptedPayload, ownedBy, eventPayload,
                                                    aggregateRootSupplier);
                                        } catch (final IOException e) {
                                            throw new EventChannelMessageHandlerException(
                                                    target, aggregateId, aggregateRootType, currentVersionInConsumption, eventType, e);
                                        }
                                    });
                        },
                        () -> LOGGER.fine("Message from target '%s' - aggregateId '%s' - aggregateRootType '%s' - aggregateVersion '%s' - eventType '%s' already consumed"));
        idempotencyRepository.upsert(target, aggregateRootType, aggregateId, new LastConsumedAggregateVersion(currentVersionInConsumption.version()));
    }
}
