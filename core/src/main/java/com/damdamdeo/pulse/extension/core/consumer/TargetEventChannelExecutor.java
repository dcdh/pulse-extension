package com.damdamdeo.pulse.extension.core.consumer;

public interface TargetEventChannelExecutor<T> {

    void execute(Target target, ApplicationNaming source, EventKey eventKey, EventRecord eventRecord, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Target target, ApplicationNaming source, EventKey eventKey, EventRecord eventRecord);

    void onAlreadyConsumed(Target target, ApplicationNaming source, EventKey eventKey, EventRecord eventRecord);
}
