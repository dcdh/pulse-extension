package com.damdamdeo.pulse.extension.core.consumer;

import java.util.List;

public interface AsyncEventChannelMessageHandlerProvider<T> {

    List<AsyncEventChannelMessageHandler<T>> provideForTarget(Target target);
}
