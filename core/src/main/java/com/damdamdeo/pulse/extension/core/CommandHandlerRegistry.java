package com.damdamdeo.pulse.extension.core;

import java.util.function.Supplier;

public interface CommandHandlerRegistry {

    <A extends AggregateRoot<?>> A execute(final AggregateId id, final Supplier<A> commandLogic);
}
