package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;

import java.util.function.Supplier;

public final class NoOpTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(Supplier<A> callable) {
        return callable.get();
    }
}
