package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.Optional;

public interface QueryEventStore<A extends AggregateRoot<K>, K extends AggregateId> {

    Optional<A> findById(final K id);

    Optional<A> findByIdAndVersion(final K id, final AggregateVersion aggregateVersion);
}
