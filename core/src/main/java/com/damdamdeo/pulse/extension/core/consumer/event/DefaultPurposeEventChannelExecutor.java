package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.checker.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.UnknownPassphraseException;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.OwnedByExecutedByDecoder;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class DefaultPurposeEventChannelExecutor<T> implements PurposeEventChannelExecutor<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final DecryptionService decryptionService;
    private final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper;
    private final AggregateRootLoader<T> aggregateRootLoader;
    private final AsyncEventChannelMessageHandlerProvider<T> asyncEventChannelMessageHandlerProvider;
    private final SequentialEventChecker sequentialEventChecker;
    private final OwnedByExecutedByDecoder ownedByExecutedByDecoder;

    public DefaultPurposeEventChannelExecutor(final DecryptionService decryptionService,
                                              final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper,
                                              final AggregateRootLoader<T> aggregateRootLoader,
                                              final AsyncEventChannelMessageHandlerProvider<T> asyncEventChannelMessageHandlerProvider,
                                              final SequentialEventChecker sequentialEventChecker,
                                              final OwnedByExecutedByDecoder ownedByExecutedByDecoder) {
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.decryptedPayloadToPayloadMapper = Objects.requireNonNull(decryptedPayloadToPayloadMapper);
        this.aggregateRootLoader = Objects.requireNonNull(aggregateRootLoader);
        this.asyncEventChannelMessageHandlerProvider = Objects.requireNonNull(asyncEventChannelMessageHandlerProvider);
        this.sequentialEventChecker = Objects.requireNonNull(sequentialEventChecker);
        this.ownedByExecutedByDecoder = Objects.requireNonNull(ownedByExecutedByDecoder);
    }

    @Override
    public void execute(final Purpose purpose, final FromApplication fromApplication, final EventKey eventKey, final EventValue eventValue, final LastConsumedAggregateVersion lastConsumedAggregateVersion) {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        Objects.requireNonNull(lastConsumedAggregateVersion);
        final CurrentVersionInConsumption currentVersionInConsumption = eventKey.toCurrentVersionInConsumption();
        sequentialEventChecker.check(lastConsumedAggregateVersion, currentVersionInConsumption);
        execute(purpose, fromApplication, eventKey, eventValue, currentVersionInConsumption);
    }

    @Override
    public void execute(final Purpose purpose, final FromApplication fromApplication, final EventKey eventKey, final EventValue eventValue) {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        Validate.validState(eventKey.toCurrentVersionInConsumption().isFirstEvent(), "Event must be first event !");
        execute(purpose, fromApplication, eventKey, eventValue, eventKey.toCurrentVersionInConsumption());
    }

    private void execute(final Purpose purpose, final FromApplication fromApplication,
                         final EventKey eventKey, final EventValue eventValue,
                         final CurrentVersionInConsumption currentVersionInConsumption) {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        Objects.requireNonNull(currentVersionInConsumption);
        final AggregateRootType aggregateRootType = eventKey.toAggregateRootType();
        final AggregateId aggregateId = eventKey.toAggregateId();
        asyncEventChannelMessageHandlerProvider.provideForTarget(purpose)
                .forEach(asyncEventChannelMessageHandler -> {
                    final Instant creationDate = eventValue.toCreationDate();
                    final EventType eventType = eventValue.toEventType();
                    final EncryptedPayload encryptedPayload = eventValue.toEncryptedEventPayload();
                    final OwnedBy ownedBy = eventValue.toOwnedBy();
                    final BelongsTo belongsTo = eventValue.toBelongsTo();
                    final ExecutedBy executedBy = eventValue.toExecutedBy(ownedByExecutedByDecoder.executedByDecoder(ownedBy));
                    try {
                        DecryptablePayload<T> decryptableEventPayload;
                        try {
                            final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, ownedBy);
                            final T decryptedEventPayload = decryptedPayloadToPayloadMapper.map(decryptedPayload);
                            decryptableEventPayload = DecryptablePayload.ofDecrypted(decryptedEventPayload);
                        } catch (final UnknownPassphraseException unknownPassphraseException) {
                            decryptableEventPayload = DecryptablePayload.ofUndecryptable();
                        }
                        final Supplier<AggregateRootLoaded<T>> aggregateRootSupplier = () -> aggregateRootLoader.getByApplicationNamingAndAggregateRootTypeAndAggregateId(fromApplication, aggregateRootType, aggregateId);
                        synchronized (this) {
                            asyncEventChannelMessageHandler.handleMessage(fromApplication, purpose, aggregateRootType, aggregateId, currentVersionInConsumption, creationDate, eventType, encryptedPayload, ownedBy,
                                    belongsTo, executedBy, decryptableEventPayload, aggregateRootSupplier);
                        }
                    } catch (final IOException e) {
                        throw new EventChannelMessageHandlerException(
                                purpose, aggregateId, aggregateRootType, currentVersionInConsumption, eventType, e);
                    }
                });
    }

    @Override
    public void onAlreadyConsumed(final Purpose purpose, final FromApplication fromApplication, final EventKey eventKey, final EventValue eventValue) {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        LOGGER.fine("Message from target '%s' - applicationNaming '%s' - aggregateRootType '%s' - aggregateId '%s' - currentVersionInConsumption '%s' - eventType '%s' already consumed"
                .formatted(purpose.name(), fromApplication.value(), eventKey.toAggregateRootType().type(), eventKey.toAggregateId().id(), eventKey.toCurrentVersionInConsumption().version(), eventValue.toEventType()));
    }
}
