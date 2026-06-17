package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.TechnicalException;

public interface UseCase<I, O> {

    O execute(I input) throws BusinessException, TechnicalException;
}
