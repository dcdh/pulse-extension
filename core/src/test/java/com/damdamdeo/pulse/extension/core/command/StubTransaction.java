package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;

import java.util.function.Supplier;

public final class StubTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A requiringNew(final Supplier<A> callable) {
        return callable.get();
    }

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(final Supplier<A> callable) {
        return callable.get();
    }
}
