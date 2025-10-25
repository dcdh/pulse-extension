package com.damdamdeo.pulse.extension.it;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.consumer.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.runtime.consumer.EventChannel;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.function.Supplier;

@ApplicationScoped
@EventChannel(target = "statistics")
public final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

    @Override
    public void handleMessage(final Target target,
                              final AggregateId aggregateId,
                              final AggregateRootType aggregateRootType,
                              final CurrentVersionInConsumption currentVersionInConsumption,
                              final Instant creationDate,
                              final EventType eventType,
                              final EncryptedPayload encryptedPayload,
                              final OwnedBy ownedBy,
                              final JsonNode decryptedEventPayload,
                              final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
    }
}
