package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.UnableToExecuteException;

public interface PurposeEventChannelExecutor<T> {

    void execute(Purpose purpose, FromApplication source, EventKey eventKey, EventValue eventValue,
                 LastConsumedAggregateVersion lastConsumedAggregateVersion) throws UnableToExecuteException;

    void execute(Purpose purpose, FromApplication source, EventKey eventKey, EventValue eventValue)
            throws UnableToExecuteException;

    void onAlreadyConsumed(Purpose purpose, FromApplication source, EventKey eventKey, EventValue eventValue);
}
