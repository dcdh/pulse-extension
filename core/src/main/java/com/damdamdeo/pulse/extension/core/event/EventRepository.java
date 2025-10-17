package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.List;
import java.util.Optional;

public interface EventRepository<A extends AggregateRoot<K>, K extends AggregateId> {

    void save(List<VersionizedEvent<K>> events) throws EventStoreException;

    List<Event<K>> loadOrderByVersionASC(K id) throws EventStoreException;

    Optional<AggregateVersion> getLastVersionByAggregateId(AggregateId aggregateId) throws EventStoreException;
}
