package com.damdamdeo.pulse.extension.consumer.deployment.event;

import com.damdamdeo.pulse.extension.consumer.runtime.event.AsyncEventConsumerChannel;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.event.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.event.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.function.Supplier;

@ApplicationScoped
@AsyncEventConsumerChannel(
        target = "statistics",
        sources = {
                @AsyncEventConsumerChannel.Source(functionalDomain = "TodoTaking", componentName = "Todo"),
                @AsyncEventConsumerChannel.Source(functionalDomain = "TodoClient", componentName = "Registered")})
public class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

    private Call call = null;

    @Override
    public void handleMessage(final FromApplication fromApplication,
                              final Target target,
                              final AggregateRootType aggregateRootType,
                              final AggregateId aggregateId,
                              final CurrentVersionInConsumption currentVersionInConsumption,
                              final Instant creationDate,
                              final EventType eventType,
                              final EncryptedPayload encryptedPayload,
                              final OwnedBy ownedBy,
                              final BelongsTo belongsTo,
                              final ExecutedBy executedBy,
                              final DecryptablePayload<JsonNode> decryptableEventPayload,
                              final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
        this.call = new Call(fromApplication,
                target, aggregateRootType, aggregateId, currentVersionInConsumption, creationDate, eventType,
                encryptedPayload, ownedBy, belongsTo, executedBy, decryptableEventPayload,
                aggregateRootLoadedSupplier.get());
    }

    public Call getCall() {
        return call;
    }

    public void reset() {
        call = null;
    }
}
