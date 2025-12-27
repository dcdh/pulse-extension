package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.consumer.idempotency.LastConsumedAggregateVersion;

public interface TargetEventChannelExecutor<T> {

    void execute(Target target, FromApplication source, EventKey eventKey, EventValue eventValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Target target, FromApplication source, EventKey eventKey, EventValue eventValue);

    void onAlreadyConsumed(Target target, FromApplication source, EventKey eventKey, EventValue eventValue);
}
