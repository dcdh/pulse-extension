package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.consumer.Purpose;

import java.util.List;

public interface AsyncEventChannelMessageHandlerProvider<T> {

    List<AsyncEventChannelMessageHandler<T>> provideForTarget(Purpose purpose);
}
