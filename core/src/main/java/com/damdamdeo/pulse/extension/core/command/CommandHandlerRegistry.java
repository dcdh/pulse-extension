package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BusinessException;

public interface CommandHandlerRegistry {

    <A extends AggregateRoot<?>> A execute(final AggregateId id, final BusinessCallable<A> commandLogic) throws BusinessException;
}
