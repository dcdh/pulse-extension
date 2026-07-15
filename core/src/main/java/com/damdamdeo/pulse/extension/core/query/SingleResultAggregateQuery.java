package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;

public interface SingleResultAggregateQuery<I extends Input> {
    String query(Passphrase passphrase, AggregateId aggregateId, I input);
}
