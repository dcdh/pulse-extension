package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;

import java.util.function.Supplier;

public interface CommandHandlerRegistry {

    <A extends AggregateRoot<?>> A execute(final AggregateId id, final Supplier<A> commandLogic);
}
