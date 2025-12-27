package com.damdamdeo.pulse.extension.core.consumer;

public interface TargetEventChannelExecutor<T> {

    void execute(Target target, FromApplication source, EventKey eventKey, EventValue eventValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Target target, FromApplication source, EventKey eventKey, EventValue eventValue);

    void onAlreadyConsumed(Target target, FromApplication source, EventKey eventKey, EventValue eventValue);
}
