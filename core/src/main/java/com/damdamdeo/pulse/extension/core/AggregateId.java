package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

public interface AggregateId extends Identifiable {

    String SEPARATOR = "-";

    String id();
}
