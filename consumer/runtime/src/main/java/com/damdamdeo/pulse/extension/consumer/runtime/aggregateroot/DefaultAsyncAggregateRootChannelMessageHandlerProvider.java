package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AsyncAggregateRootChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AsyncAggregateRootChannelMessageHandlerProvider;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
@Unremovable
@DefaultBean
public final class DefaultAsyncAggregateRootChannelMessageHandlerProvider implements AsyncAggregateRootChannelMessageHandlerProvider<JsonNode> {

    private final Instance<AsyncAggregateRootChannelMessageHandler<JsonNode>> aggregateRootChannelMessageHandlers;

    private final Map<Target, List<AsyncAggregateRootChannelMessageHandler<JsonNode>>> cache;

    public DefaultAsyncAggregateRootChannelMessageHandlerProvider(@Any final Instance<AsyncAggregateRootChannelMessageHandler<JsonNode>> aggregateRootChannelMessageHandlers) {
        this.aggregateRootChannelMessageHandlers = Objects.requireNonNull(aggregateRootChannelMessageHandlers);
        this.cache = new HashMap<>();
    }

    @Override
    public List<AsyncAggregateRootChannelMessageHandler<JsonNode>> provideForTarget(final Target target) {
        Objects.requireNonNull(target);
        return cache.computeIfAbsent(target, target1 ->
                aggregateRootChannelMessageHandlers.select(AsyncAggregateRootConsumerChannel.Literal.of(target1.name())).stream().toList());
    }
}
