package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;

/**
 * Implementations should be injectable
 *
 * @param <K>
 * @param <C>
 */
public abstract non-sealed class SpecificDomain<K extends AggregateId, C extends Command<K>> implements Permission<K, C> {

    @Override
    public final int priority() {
        return 7;
    }
}
