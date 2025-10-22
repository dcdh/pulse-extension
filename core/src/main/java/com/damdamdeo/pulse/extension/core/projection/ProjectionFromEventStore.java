package com.damdamdeo.pulse.extension.core.projection;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.List;
import java.util.Optional;

public interface ProjectionFromEventStore<P extends Projection> {

    Optional<P> findBy(OwnedBy ownedBy, AggregateId aggregateId, SingleResultAggregateQuery singleResultAggregateQuery) throws ProjectionException;

    List<P> findAll(OwnedBy ownedBy, MultipleResultAggregateQuery multipleResultAggregateQuery) throws ProjectionException;
}
