package com.damdamdeo.pulse.extension.core;

import java.util.List;

public interface EventRepository<A extends AggregateRoot<K>, K extends AggregateId> {

    void save(List<VersionizedEvent<K>> events) throws EventStoreException;

    List<Event<K>> loadOrderByVersionASC(K id) throws EventStoreException;
}
