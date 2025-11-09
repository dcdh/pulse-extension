package com.damdamdeo.pulse.extension.it;

import com.damdamdeo.pulse.extension.consumer.runtime.EventChannel;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.function.Supplier;

@ApplicationScoped
@EventChannel(target = "statistics",
        sources = {
                @EventChannel.Source(functionalDomain = "TodoTaking", componentName = "Todo")
        })
public final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

    private Call call = null;

    @Override
    public void handleMessage(final Target target,
                              final AggregateRootType aggregateRootType,
                              final AggregateId aggregateId,
                              final CurrentVersionInConsumption currentVersionInConsumption,
                              final Instant creationDate,
                              final EventType eventType,
                              final EncryptedPayload encryptedPayload,
                              final OwnedBy ownedBy,
                              final DecryptablePayload<JsonNode> decryptableEventPayload,
                              final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
        this.call = new Call(
                target,
                aggregateRootType,
                aggregateId,
                currentVersionInConsumption,
                creationDate,
                eventType,
                encryptedPayload,
                ownedBy,
                decryptableEventPayload,
                aggregateRootLoadedSupplier.get());
    }

    public Call getCall() {
        return call;
    }
}
