package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;

public final class StubTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<K>, K extends AggregateId> Handled<A, K> requiringNew(final CommandCallable<Handled<A, K>> callable) throws CommandException {
        return callable.call();
    }

    @Override
    public <A extends AggregateRoot<K>, K extends AggregateId> Handled<A, K> joiningExisting(final CommandCallable<Handled<A, K>> callable) throws CommandException {
        return callable.call();
    }
}
