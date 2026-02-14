package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.BusinessException;

public interface BusinessExceptionToHttpProblemDetailMapper {

    String toDetail(final BusinessException exception);
}
