package com.damdamdeo.pulse.extension.core.consumer;

public interface EventKey {

    String aggregateRootId();

    String aggregateRootType();

    Integer version();
}
