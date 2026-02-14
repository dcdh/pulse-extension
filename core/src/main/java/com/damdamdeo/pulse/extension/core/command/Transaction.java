package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BusinessException;

public interface Transaction {

    <A extends AggregateRoot<?>> A requiringNew(BusinessCallable<A> callable) throws BusinessException;

    <A extends AggregateRoot<?>> A joiningExisting(BusinessCallable<A> callable) throws BusinessException;
}
