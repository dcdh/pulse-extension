package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.BusinessException;

@FunctionalInterface
public interface BusinessCallable<T> {

    T call() throws BusinessException;
}
