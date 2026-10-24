package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.UserId;

public interface UserValidity {

    boolean isValid(UserId userId);
}
