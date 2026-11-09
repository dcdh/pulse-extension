package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;

/**
 * Implementations should be injectable
 *
 * @param <K>
 */
public abstract non-sealed class SpecificQuery<A extends AggregateId> implements CompositePermission<A> {

}
