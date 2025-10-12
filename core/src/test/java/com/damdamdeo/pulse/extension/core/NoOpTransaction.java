package com.damdamdeo.pulse.extension.core;

import java.util.function.Supplier;

public final class NoOpTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(Supplier<A> callable) {
        return callable.get();
    }
}
