package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.BusinessException;

public interface GenericUseCase<I, O> {

    O execute(I input) throws BusinessException;
}
