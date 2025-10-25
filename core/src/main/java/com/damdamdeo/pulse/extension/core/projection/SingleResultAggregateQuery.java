package com.damdamdeo.pulse.extension.core.projection;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;

public interface SingleResultAggregateQuery {
    String query(Passphrase passphrase, AggregateId aggregateId);
}
