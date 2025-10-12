package com.damdamdeo.pulse.extension.runtime;

import java.util.List;

public interface EventRepository<A extends AggregateRoot<K>, K extends AggregateId<?>> {

    void save(Class<A> aggregateRoot, List<VersionizedEvent<K>> events);

    List<Event<K>> loadOrderByVersionASC(Class<A> aggregateRoot, K id);
}
