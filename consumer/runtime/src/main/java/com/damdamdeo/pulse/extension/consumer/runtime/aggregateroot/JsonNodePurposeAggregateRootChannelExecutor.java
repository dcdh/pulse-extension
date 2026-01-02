package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.DecryptedPayloadToPayloadMapper;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AsyncAggregateRootChannelMessageHandlerProvider;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.DefaultPurposeAggregateRootChannelExecutor;
import com.damdamdeo.pulse.extension.core.consumer.checker.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

@Singleton
@Unremovable
@DefaultBean
public final class JsonNodePurposeAggregateRootChannelExecutor extends DefaultPurposeAggregateRootChannelExecutor<JsonNode> {

    public JsonNodePurposeAggregateRootChannelExecutor(final DecryptionService decryptionService,
                                                       final DecryptedPayloadToPayloadMapper<JsonNode> decryptedPayloadToPayloadMapper,
                                                       final AsyncAggregateRootChannelMessageHandlerProvider<JsonNode> asyncAggregateRootChannelMessageHandlerProvider,
                                                       final SequentialEventChecker sequentialEventChecker) {
        super(decryptionService, decryptedPayloadToPayloadMapper, asyncAggregateRootChannelMessageHandlerProvider, sequentialEventChecker);
    }
}
