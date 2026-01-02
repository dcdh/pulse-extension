package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;

public interface PurposeEventChannelExecutor<T> {

    void execute(Purpose purpose, FromApplication source, EventKey eventKey, EventValue eventValue, LastConsumedAggregateVersion lastConsumedAggregateVersion);

    void execute(Purpose purpose, FromApplication source, EventKey eventKey, EventValue eventValue);

    void onAlreadyConsumed(Purpose purpose, FromApplication source, EventKey eventKey, EventValue eventValue);
}
