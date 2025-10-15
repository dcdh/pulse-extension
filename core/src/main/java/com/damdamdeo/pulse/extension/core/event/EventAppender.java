package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface EventAppender<K extends AggregateId> {
    void append(Event<K> event);
}
