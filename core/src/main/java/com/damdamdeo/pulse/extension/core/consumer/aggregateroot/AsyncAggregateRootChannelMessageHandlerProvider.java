package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.Purpose;

import java.util.List;

public interface AsyncAggregateRootChannelMessageHandlerProvider<T> {

    List<AsyncAggregateRootChannelMessageHandler<T>> provideForTarget(Purpose purpose);
}
