package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;

public interface PurposeAggregateRootChannelExecutor<T> {

    void execute(Purpose purpose, FromApplication source, AggregateRootKey aggregateRootKey, AggregateRootValue aggregateRootValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Purpose purpose, FromApplication source, AggregateRootKey aggregateRootKey, AggregateRootValue aggregateRootValue);

    void onAlreadyConsumed(Purpose purpose, FromApplication source, AggregateRootKey aggregateRootKey, AggregateRootValue aggregateRootValue);
}
