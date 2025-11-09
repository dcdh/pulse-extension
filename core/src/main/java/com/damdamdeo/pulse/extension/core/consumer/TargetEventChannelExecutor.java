package com.damdamdeo.pulse.extension.core.consumer;

public interface TargetEventChannelExecutor<T> {

    void execute(Target target, ApplicationNaming source, EventKey eventKey, EventValue eventValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Target target, ApplicationNaming source, EventKey eventKey, EventValue eventValue);

    void onAlreadyConsumed(Target target, ApplicationNaming source, EventKey eventKey, EventValue eventValue);
}
