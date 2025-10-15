package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;

import java.util.function.Supplier;

public interface Transaction {

    <A extends AggregateRoot<?>> A joiningExisting(Supplier<A> callable);
}
