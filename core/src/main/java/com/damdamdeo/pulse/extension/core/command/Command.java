package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface Command<K extends AggregateId> {
    K id();
}
