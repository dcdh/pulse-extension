package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class DefaultTargetEventChannelExecutor<T> implements TargetEventChannelExecutor<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final DecryptionService decryptionService;
    private final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper;
    private final AggregateRootLoader<T> aggregateRootLoader;
    private final AsyncEventChannelMessageHandlerProvider<T> asyncEventChannelMessageHandlerProvider;
    private final SequentialEventChecker sequentialEventChecker;

    public DefaultTargetEventChannelExecutor(final DecryptionService decryptionService,
                                             final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper,
                                             final AggregateRootLoader<T> aggregateRootLoader,
                                             final AsyncEventChannelMessageHandlerProvider<T> asyncEventChannelMessageHandlerProvider,
                                             final SequentialEventChecker sequentialEventChecker) {
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.decryptedPayloadToPayloadMapper = Objects.requireNonNull(decryptedPayloadToPayloadMapper);
        this.aggregateRootLoader = Objects.requireNonNull(aggregateRootLoader);
        this.asyncEventChannelMessageHandlerProvider = Objects.requireNonNull(asyncEventChannelMessageHandlerProvider);
        this.sequentialEventChecker = Objects.requireNonNull(sequentialEventChecker);
    }

    @Override
    public void execute(final Target target, final ApplicationNaming applicationNaming, final EventKey eventKey, final EventRecord eventRecord, final LastConsumedAggregateVersion lastConsumedAggregateVersion) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(applicationNaming);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventRecord);
        Objects.requireNonNull(lastConsumedAggregateVersion);
        final CurrentVersionInConsumption currentVersionInConsumption = eventRecord.toCurrentVersionInConsumption();
        sequentialEventChecker.check(lastConsumedAggregateVersion, currentVersionInConsumption);
        execute(target, applicationNaming, eventRecord, currentVersionInConsumption);
    }

    @Override
    public void execute(final Target target, final ApplicationNaming applicationNaming, final EventKey eventKey, final EventRecord eventRecord) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(applicationNaming);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventRecord);
        Validate.validState(eventRecord.match(eventKey), "Key mismatch with payload !");
        Validate.validState(eventKey.toCurrentVersionInConsumption().isFirstEvent(), "Event must be first event !");
        execute(target, applicationNaming, eventRecord, eventKey.toCurrentVersionInConsumption());
    }

    private void execute(final Target target, final ApplicationNaming applicationNaming, final EventRecord eventRecord, final CurrentVersionInConsumption currentVersionInConsumption) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(applicationNaming);
        Objects.requireNonNull(eventRecord);
        Objects.requireNonNull(currentVersionInConsumption);
        final AggregateRootType aggregateRootType = eventRecord.toAggregateRootType();
        final AggregateId aggregateId = eventRecord.toAggregateId();
        asyncEventChannelMessageHandlerProvider.provideForTarget(target)
                .forEach(asyncEventChannelMessageHandler -> {
                    final Instant creationDate = eventRecord.toCreationDate();
                    final EventType eventType = eventRecord.toEventType();
                    final EncryptedPayload encryptedPayload = eventRecord.toEncryptedEventPayload();
                    final OwnedBy ownedBy = eventRecord.toOwnedBy();
                    try {
                        final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, ownedBy);
                        final T decryptedEventPayload = decryptedPayloadToPayloadMapper.map(decryptedPayload);
                        final Supplier<AggregateRootLoaded<T>> aggregateRootSupplier = () -> aggregateRootLoader.getByApplicationNamingAndAggregateRootTypeAndAggregateId(applicationNaming, aggregateRootType, aggregateId);
                        asyncEventChannelMessageHandler.handleMessage(target, aggregateRootType, aggregateId, currentVersionInConsumption, creationDate, eventType, encryptedPayload, ownedBy, decryptedEventPayload,
                                aggregateRootSupplier);
                    } catch (final IOException e) {
                        throw new EventChannelMessageHandlerException(
                                target, aggregateId, aggregateRootType, currentVersionInConsumption, eventType, e);
                    }
                });
    }

    @Override
    public void onAlreadyConsumed(final Target target, final ApplicationNaming applicationNaming, final EventKey eventKey, final EventRecord eventRecord) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventRecord);
        LOGGER.fine("Message from target '%s' - applicationNaming '%s' - aggregateRootType '%s' - aggregateId '%s' - currentVersionInConsumption '%s' - eventType '%s' already consumed"
                .formatted(target.name(), applicationNaming.value(), eventKey.toAggregateRootType().type(), eventKey.toAggregateId().id(), eventKey.toCurrentVersionInConsumption().version(), eventRecord.toEventType()));
    }
}
