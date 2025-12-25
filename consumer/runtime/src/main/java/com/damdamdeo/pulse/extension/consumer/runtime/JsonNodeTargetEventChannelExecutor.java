package com.damdamdeo.pulse.extension.consumer.runtime;

import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.executedby.OwnedByExecutedByDecoder;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

@Singleton
@Unremovable
@DefaultBean
public final class JsonNodeTargetEventChannelExecutor extends DefaultTargetEventChannelExecutor<JsonNode> {

    public JsonNodeTargetEventChannelExecutor(final DecryptionService decryptionService,
                                              final DecryptedPayloadToPayloadMapper<JsonNode> decryptedPayloadToPayloadMapper,
                                              final AggregateRootLoader<JsonNode> aggregateRootLoader,
                                              final AsyncEventChannelMessageHandlerProvider<JsonNode> asyncEventChannelMessageHandlerProvider,
                                              final SequentialEventChecker sequentialEventChecker,
                                              final OwnedByExecutedByDecoder ownedByExecutedByDecoder) {
        super(decryptionService, decryptedPayloadToPayloadMapper, aggregateRootLoader, asyncEventChannelMessageHandlerProvider,
                sequentialEventChecker, ownedByExecutedByDecoder);
    }
}
