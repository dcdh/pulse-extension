package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

/**
 * Implementations should be injectable
 *
 * @param <K>
 */
public abstract non-sealed class SpecificQuery<K extends Identifiable> implements CompositePermission<K> {

}
