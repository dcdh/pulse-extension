package com.damdamdeo.pulse.extension.core.projection;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface SingleResultAggregateQuery {
    String query(char[] passphrase, AggregateId aggregateId);
}
