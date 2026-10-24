package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;

/**
 * Implementations should be injectable
 *
 * @param <K>
 */
public abstract non-sealed class SpecificQuery<K extends AggregateId> implements CompositePermission<K> {

}
