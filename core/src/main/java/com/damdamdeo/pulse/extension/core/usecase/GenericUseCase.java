package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.TechnicalException;

public interface GenericUseCase<I, O> {

    O execute(I input) throws BusinessException, TechnicalException;
}
