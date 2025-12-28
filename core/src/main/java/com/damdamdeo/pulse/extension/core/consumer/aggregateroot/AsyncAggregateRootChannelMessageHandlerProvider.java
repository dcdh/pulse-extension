package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.Target;

import java.util.List;

public interface AsyncAggregateRootChannelMessageHandlerProvider<T> {

    List<AsyncAggregateRootChannelMessageHandler<T>> provideForTarget(Target target);
}
