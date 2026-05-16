package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;

public  record UserAggregateId() implements AggregateId {

    @Override
    public String id() {
        return "U-000001";
    }
}
