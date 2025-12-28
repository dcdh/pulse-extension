package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.checker.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.UnknownPassphraseException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class DefaultTargetAggregateRootChannelExecutor<T> implements TargetAggregateRootChannelExecutor<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final DecryptionService decryptionService;
    private final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper;
    private final AsyncAggregateRootChannelMessageHandlerProvider<T> asyncAggregateRootChannelMessageHandlerProvider;
    private final SequentialEventChecker sequentialEventChecker;

    public DefaultTargetAggregateRootChannelExecutor(final DecryptionService decryptionService,
                                                     final DecryptedPayloadToPayloadMapper<T> decryptedPayloadToPayloadMapper,
                                                     final AsyncAggregateRootChannelMessageHandlerProvider<T> asyncAggregateRootChannelMessageHandlerProvider,
                                                     final SequentialEventChecker sequentialEventChecker) {
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.decryptedPayloadToPayloadMapper = Objects.requireNonNull(decryptedPayloadToPayloadMapper);
        this.asyncAggregateRootChannelMessageHandlerProvider = Objects.requireNonNull(asyncAggregateRootChannelMessageHandlerProvider);
        this.sequentialEventChecker = Objects.requireNonNull(sequentialEventChecker);
    }

    @Override
    public void execute(final Target target, final FromApplication fromApplication,
                        final AggregateRootKey aggregateRootKey, final AggregateRootValue aggregateRootValue,
                        final LastConsumedAggregateVersion lastConsumedAggregateVersion) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootKey);
        Objects.requireNonNull(aggregateRootValue);
        Objects.requireNonNull(lastConsumedAggregateVersion);
        final CurrentVersionInConsumption currentVersionInConsumption = aggregateRootKey.toCurrentVersionInConsumption();
        sequentialEventChecker.check(lastConsumedAggregateVersion, currentVersionInConsumption);
        execute(target, fromApplication, aggregateRootKey, aggregateRootValue, currentVersionInConsumption);
    }

    @Override
    public void execute(final Target target, final FromApplication fromApplication,
                        final AggregateRootKey aggregateRootKey, final AggregateRootValue aggregateRootValue) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootKey);
        Objects.requireNonNull(aggregateRootValue);
        Validate.validState(aggregateRootKey.toCurrentVersionInConsumption().isFirstEvent(), "Event must be first event !");
        execute(target, fromApplication, aggregateRootKey, aggregateRootValue, aggregateRootKey.toCurrentVersionInConsumption());
    }

    private void execute(final Target target, final FromApplication fromApplication,
                         final AggregateRootKey aggregateRootKey, final AggregateRootValue aggregateRootValue,
                         final CurrentVersionInConsumption currentVersionInConsumption) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootKey);
        Objects.requireNonNull(aggregateRootValue);
        Objects.requireNonNull(currentVersionInConsumption);
        final AggregateRootType aggregateRootType = aggregateRootKey.toAggregateRootType();
        final AggregateId aggregateId = aggregateRootKey.toAggregateId();
        asyncAggregateRootChannelMessageHandlerProvider.provideForTarget(target)
                .forEach(asyncEventChannelMessageHandler -> {
                    final EncryptedPayload encryptedPayload = aggregateRootValue.toEncryptedPayload();
                    final OwnedBy ownedBy = aggregateRootValue.toOwnedBy();
                    final BelongsTo belongsTo = aggregateRootValue.toBelongsTo();
                    try {
                        DecryptablePayload<T> decryptableEventPayload;
                        try {
                            final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, ownedBy);
                            final T decryptedAggregateRootPayload = decryptedPayloadToPayloadMapper.map(decryptedPayload);
                            decryptableEventPayload = DecryptablePayload.ofDecrypted(decryptedAggregateRootPayload);
                        } catch (final UnknownPassphraseException unknownPassphraseException) {
                            decryptableEventPayload = DecryptablePayload.ofUndecryptable();
                        }
                        synchronized (this) {
                            asyncEventChannelMessageHandler.handleMessage(fromApplication, target, aggregateRootType, aggregateId, currentVersionInConsumption, encryptedPayload, ownedBy,
                                    belongsTo, decryptableEventPayload);
                        }
                    } catch (final IOException e) {
                        throw new AggregateRootChannelMessageHandlerException(
                                target, aggregateId, aggregateRootType, currentVersionInConsumption, e);
                    }
                });
    }

    @Override
    public void onAlreadyConsumed(final Target target, final FromApplication fromApplication,
                                  final AggregateRootKey aggregateRootKey, final AggregateRootValue aggregateRootValue) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootKey);
        Objects.requireNonNull(aggregateRootValue);
        LOGGER.fine("Message from target '%s' - applicationNaming '%s' - aggregateRootType '%s' - aggregateId '%s' - currentVersionInConsumption '%s' already consumed"
                .formatted(target.name(), fromApplication.value(), aggregateRootKey.toAggregateRootType().type(), aggregateRootKey.toAggregateId().id(),
                        aggregateRootKey.toCurrentVersionInConsumption().version()));
    }
}
