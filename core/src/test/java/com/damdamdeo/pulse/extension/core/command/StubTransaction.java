package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BusinessException;

public final class StubTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A requiringNew(final BusinessCallable<A> callable) throws BusinessException {
        return callable.call();
    }

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(final BusinessCallable<A> callable) throws BusinessException {
        return callable.call();
    }
}
