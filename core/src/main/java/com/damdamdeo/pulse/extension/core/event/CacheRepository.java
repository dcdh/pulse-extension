package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.Optional;

public interface CacheRepository<A extends AggregateRoot<K>, K extends AggregateId> {

    Optional<CachedAggregateRoot<A>> getById(K id);

    void store(A aggregateRoot, AggregateVersion aggregateVersion);
}
