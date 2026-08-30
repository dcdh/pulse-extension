package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;

public final class StubTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A requiringNew(final CommandCallable<A> callable) throws CommandException {
        return callable.call();
    }

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(final CommandCallable<A> callable) throws CommandException {
        return callable.call();
    }
}
