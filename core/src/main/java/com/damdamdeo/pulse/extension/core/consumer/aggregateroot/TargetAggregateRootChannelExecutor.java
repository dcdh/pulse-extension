package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.Target;

public interface TargetAggregateRootChannelExecutor<T> {

    void execute(Target target, FromApplication source, AggregateRootKey aggregateRootKey, AggregateRootValue aggregateRootValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Target target, FromApplication source, AggregateRootKey aggregateRootKey, AggregateRootValue aggregateRootValue);

    void onAlreadyConsumed(Target target, FromApplication source, AggregateRootKey aggregateRootKey, AggregateRootValue aggregateRootValue);
}
