package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

public interface ProjectionFromEventStore<I extends Input, P extends Projection> {

    Optional<Result<P>> findBy(OwnedBy ownedBy, AggregateId aggregateId, I input, SingleResultAggregateQuery<I> singleResultAggregateQuery) throws ProjectionException;

    Result<P> findAll(OwnedBy ownedBy, I input, MultipleResultAggregateQuery<I> multipleResultAggregateQuery) throws ProjectionException;
}
