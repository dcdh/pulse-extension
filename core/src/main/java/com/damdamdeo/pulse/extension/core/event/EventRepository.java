package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.VersionizedAggregateRoot;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.List;
import java.util.Optional;

public interface EventRepository<A extends AggregateRoot<K>, K extends AggregateId> {

    void save(List<VersionizedEvent<K>> events, AggregateRoot<K> aggregateRoot, ExecutedBy executed_by) throws EventStoreException;

    List<ExecutedByEvent<K>> loadOrderByVersionASC(K id) throws EventStoreException;

    List<ExecutedByEvent<K>> loadOrderByVersionASC(K id, AggregateVersion aggregateVersionRequested) throws EventStoreException;

    Optional<VersionizedAggregateRoot<A>> findLastVersionById(K id);

    Optional<AggregateVersion> findLastAggregateVersionById(K id);

    List<EventMetadata> findEventMetadataByIdOrderByVersionASC(K id);

    List<EventMetadata> findEventMetadataByIdAndEventsOrderByVersionASC(K id, List<Class<? extends Event<K>>> events);

    boolean hasEventsFor(K id);
}
