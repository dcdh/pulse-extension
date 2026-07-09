package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;

public interface SingleResultAggregateQuery {
    String query(Passphrase passphrase, AggregateId aggregateId);
}
