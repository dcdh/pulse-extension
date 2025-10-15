package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;

import java.util.List;

public interface EventRepository<A extends AggregateRoot<K>, K extends AggregateId> {

    void save(List<VersionizedEvent<K>> events) throws EventStoreException;

    List<Event<K>> loadOrderByVersionASC(K id) throws EventStoreException;
}
