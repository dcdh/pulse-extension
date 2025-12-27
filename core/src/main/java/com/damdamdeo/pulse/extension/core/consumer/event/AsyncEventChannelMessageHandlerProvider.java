package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.consumer.Target;

import java.util.List;

public interface AsyncEventChannelMessageHandlerProvider<T> {

    List<AsyncEventChannelMessageHandler<T>> provideForTarget(Target target);
}
