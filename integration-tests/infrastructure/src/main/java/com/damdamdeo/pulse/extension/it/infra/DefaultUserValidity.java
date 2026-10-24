package com.damdamdeo.pulse.extension.it.infra;

import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.usecase.permission.UserValidity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultUserValidity implements UserValidity {

    @Override
    public boolean isValid(final UserId userId) {
        throw new UnsupportedOperationException();
    }
}
