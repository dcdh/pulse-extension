package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.Target;

public interface TargetEventChannelExecutor<T> {

    void execute(Target target, FromApplication source, EventKey eventKey, EventValue eventValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Target target, FromApplication source, EventKey eventKey, EventValue eventValue);

    void onAlreadyConsumed(Target target, FromApplication source, EventKey eventKey, EventValue eventValue);
}
