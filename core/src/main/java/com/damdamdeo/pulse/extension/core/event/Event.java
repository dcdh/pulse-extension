package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface Event<K extends AggregateId> extends Ownership {
    K id();
}
