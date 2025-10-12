package com.damdamdeo.pulse.extension.runtime;

import java.util.function.Supplier;

public interface Transaction {

    <A extends AggregateRoot<?>> A joiningExisting(Supplier<A> callable);
}
