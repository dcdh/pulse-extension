package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

public interface ProjectionFromEventStore<P extends Projection> {

    Result<P> getOneByAggregateId(AggregateId aggregateId, SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException;

    Optional<Result<P>> findOneByAggregateId(AggregateId aggregateId, SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException;

    <I extends Input> Result<P> findAllBy(OwnedBy ownedBy, I input, MultipleResultProjectionQuery<I> multipleResultProjectionQuery) throws ProjectionException;
}
