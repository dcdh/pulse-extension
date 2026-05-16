package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

public interface CreationalCommand<K extends AggregateId> extends Command<K> {

    @Override
    default K id() {
        throw new UnsupportedOperationException("You must use handle(final K id, final CreationalCommand<K> creationalCommand, final Supplier<DuplicateAggregateException> duplicateAggregateExceptionSupplier)");
    }

    default Optional<OwnedBy> ownedBy() {
        return Optional.empty();
    }
}
