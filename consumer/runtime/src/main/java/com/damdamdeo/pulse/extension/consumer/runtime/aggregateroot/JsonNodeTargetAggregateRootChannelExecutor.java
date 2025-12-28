package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.DecryptedPayloadToPayloadMapper;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AsyncAggregateRootChannelMessageHandlerProvider;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.DefaultTargetAggregateRootChannelExecutor;
import com.damdamdeo.pulse.extension.core.consumer.checker.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

@Singleton
@Unremovable
@DefaultBean
public final class JsonNodeTargetAggregateRootChannelExecutor extends DefaultTargetAggregateRootChannelExecutor<JsonNode> {

    public JsonNodeTargetAggregateRootChannelExecutor(final DecryptionService decryptionService,
                                                      final DecryptedPayloadToPayloadMapper<JsonNode> decryptedPayloadToPayloadMapper,
                                                      final AsyncAggregateRootChannelMessageHandlerProvider<JsonNode> asyncAggregateRootChannelMessageHandlerProvider,
                                                      final SequentialEventChecker sequentialEventChecker) {
        super(decryptionService, decryptedPayloadToPayloadMapper, asyncAggregateRootChannelMessageHandlerProvider, sequentialEventChecker);
    }
}
