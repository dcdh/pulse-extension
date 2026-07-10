package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

public interface ProjectionFromEventStore<P extends Projection> {

    Optional<Result<P>> findBy(OwnedBy ownedBy, AggregateId aggregateId, SingleResultAggregateQuery singleResultAggregateQuery) throws ProjectionException;

    Result<P> findAll(OwnedBy ownedBy, MultipleResultAggregateQuery multipleResultAggregateQuery) throws ProjectionException;
}
