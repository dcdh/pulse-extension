package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

/**
 * Do not use, demo purpose only.
 *
 * @param <P>
 */
@Deprecated
public interface ProjectionFromEventStore<A extends AggregateId, P extends Projection<A>> {

    SingleResult<A, P> getOneByAggregateId(A aggregateId, SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException;

    Optional<SingleResult<A, P>> findOneByAggregateId(A aggregateId, SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException;

    <I extends Input> MultipleResult<A, P> findAllBy(OwnedBy ownedBy, I input, MultipleResultProjectionQuery<I> multipleResultProjectionQuery) throws ProjectionException;
}
