package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;

public abstract non-sealed class SpecificQuery<K extends AggregateId> implements Permission<K> {

    @Override
    public final int priority() {
        return 7;
    }
}
